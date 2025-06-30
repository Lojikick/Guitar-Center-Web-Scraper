package havocprod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// import org.apache.poi.ss.usermodel.Sheet;
// import org.apache.poi.ss.usermodel.Row;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;

public class GCCrawler {
    private static ArrayList<String> urls = new ArrayList<String>();
    private static Set<String> visited_urls = new HashSet<String>();
    private static List<productRecord> productData = new ArrayList<>();

    private static int maxDepth = 5;
    private static final long DELAY_BETWEEN_REQUESTS = 1000;
    private static String[] colors = {"Black","Burst%20or%20Fade","Blue","Red","White","Brown", "Multi-Colored", "Gray", "Green", "Natural", "Orange", "Pink", "Purple", "Tan"};
    private static Map<String, Integer> page_numbers  = Map.ofEntries(
        entry("Black", 28),
        entry( "Burst%20or%20Fade", 22),
        entry("Blue", 18),
        entry( "Red", 16),
        entry("White", 14),
        entry("Green", 10),
        entry( "Yellow", 9),
        entry("Multi-Colored", 6),
        entry("Gray", 6),
        entry("Natural", 6),
        entry("Orange", 3),
        entry("Pink", 3),
        entry("Purple", 5),
        entry("Tan", 2),
        entry("Brown", 3)
    );
    private static Map<String, List<productRecord>> productDataByColor = new HashMap<>();

    static {
        for (String color : colors) {
            productDataByColor.put(color, new ArrayList<>());
        }
    }

    public static void main(String[] args){
        String seedUrl = "https://www.guitarcenter.com/Solid-Body-Electric-Guitars.gc";
        
        visited_urls.add(seedUrl);
        urls.add(seedUrl);
        
        crawlByColor(seedUrl, 0);
        exportDataToCsvByColor("guitar_center_product_data.csv");

    }

    private static void crawlSingle(String url_to_crawl, int depth){
        if (depth > maxDepth){
            return;
        }

        Document doc = retrieveHTML(url_to_crawl);

        
        if (doc == null){
            System.err.println("Url is empty, skipping");
            return;
        }
        
        int last_page_num = 2;
        Element paginationContainer = doc.select("ul.ant-pagination").first();
        if (paginationContainer != null) {
            Element lastPageWithLabel = paginationContainer.select("li.ant-pagination-item").last();
            last_page_num = Integer.parseInt(lastPageWithLabel.text().trim());
            // System.out.println(last_page_num);
        } else {
            System.out.println("Couldn't find it :(");
        }

        System.out.println("Currently crawling: " + url_to_crawl);
        extractProductData(doc);

        for(int i = 2; i <= 20; i++){
            try {
                Thread.sleep(DELAY_BETWEEN_REQUESTS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            String curr_link = url_to_crawl + "?page=" + String.valueOf(i);
            
            Document curr_doc = retrieveHTML(curr_link);
            if (curr_doc == null){
                System.err.println(curr_link +" is empty, skipping");
                continue;
            }
            System.out.println("Currently crawling: " + curr_link);
            extractProductData(curr_doc);
        }
        return;
    }

    private static void crawlByColor(String url_to_crawl, int depth){
        if (depth > maxDepth){
            return;
        }

        

        for (String color : colors){

            String color_url_to_crawl = url_to_crawl + "?filters=Color:" + color;
            Document temp_doc = retrieveHTML(color_url_to_crawl);

            int last_page_num = page_numbers.get(color);
            // Element paginationContainer = temp_doc.select("ul.ant-pagination").first();

            // if (paginationContainer != null) {
            //     // System.out.println(paginationContainer);
            //     Element lastPageWithLabel = paginationContainer.select("li.ant-pagination-item").last();
            //     if (lastPageWithLabel != null) {
            //         last_page_num = Integer.parseInt(lastPageWithLabel.text().trim());
            //         System.out.println("Last page num for color" + color +": " + last_page_num);
            //     }
            // } else {
            //     System.out.println("Couldn't find it :(");
            //     continue;
            // }
            // System.out.println(last_page_num);

            for(int i = 1; i <= last_page_num; i++){
                try {
                    Thread.sleep(DELAY_BETWEEN_REQUESTS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                if (i == 1){
                    System.out.println("Currently crawling: " + color_url_to_crawl);
                    extractProductDataColor(temp_doc, color);

                } else {
                    String curr_link = color_url_to_crawl + "?page=" + String.valueOf(i);

                    Document curr_doc = retrieveHTML(curr_link);
                    if (curr_doc == null){
                        System.err.println(curr_link +" is empty, skipping");
                        continue;
                    }
                    System.out.println("Currently crawling: " + curr_link);
                    extractProductDataColor(curr_doc, color);
                }
                
            }
        }
        
        return;
    }

    private static void extractProductDataColor(Document document, String color){
        
        Elements products = document.select("section.plp-product-grid");
        
        if (products == null){
            System.err.println("Unable to fetch product elem");
            return;
        }
        for(Element product : products){
            Element product_img_element = product.select("img").first();
            Element product_name = product.select("a.product-name h2").first();
            Element product_price = product.select("span.sale-price").first();
            Element product_url_element = product.select("a[title='product img']").first();

            String product_name_text = "N/A";
            double product_price_number = 0;
            String product_img_url = "N/A";
            String product_listing_url = "N/A";

            if (product_name != null){
                product_name_text = product_name.text();
            }

            if (product_price != null){
                String product_price_init = product_price.text();
                if (product_price_init != null){
                    String replaced = product_price_init.replaceAll("[^\\d.]", "");
                    if(replaced != ""){
                        product_price_number = Double.parseDouble(product_price_init.replaceAll("[^\\d.]", ""));
                    }
                }
            }

            if (product_img_element != null){
                product_img_url = product_img_element.attr("src");
            }

            if (product_url_element != null){
                product_listing_url = "https://www.guitarcenter.com" + product_url_element.attr("href");
            }
    
            
            productRecord record = new productRecord(product_name_text, product_price_number, product_img_url, product_listing_url);
            productDataByColor.get(color).add(record);
        }
        
        return;

    }

    private static void extractProductData(Document document){
        
        Elements products = document.select("section.plp-product-grid");
        
        if (products == null){
            System.err.println("Unable to fetch product elem");
            return;
        }
        for(Element product : products){
            //Implement error checking! -- DONE
            Element product_img_element = product.select("img").first();
            Element product_name = product.select("a.product-name h2").first();
            Element product_price = product.select("span.sale-price").first();
            Element product_url_element = product.select("a[title='product img']").first();

            String product_name_text = "N/A";
            double product_price_number = 0;
            String product_img_url = "N/A";
            String product_listing_url = "N/A";

            if (product_name != null){
                product_name_text = product_name.text();
            }

            if (product_price != null){
                String product_price_init = product_price.text();
                product_price_number = Double.parseDouble(product_price_init.replaceAll("[^\\d.]", ""));
            }

            if (product_img_element != null){
                product_img_url = product_img_element.attr("src");
            }

            if (product_url_element != null){
                product_listing_url = "https://www.guitarcenter.com" + product_url_element.attr("href");
            }
    
            
            productRecord record = new productRecord(product_name_text, product_price_number, product_img_url, product_listing_url);
            productData.add(record);
        }
        
        return;

    }
    
    private static Document retrieveHTML(String url){
        try {

            Document doc = Jsoup.connect(url).get();

        return doc;

        } catch (IOException e) {
            System.err.println("Unable to fetch HTML of: " + url);
        }

        return null;
    }

    private static void crawl(String url_to_crawl, int depth){
        if (depth > maxDepth){
            return;
        }

        Document doc = retrieveHTML(url_to_crawl);
        
        if (doc == null){
            System.err.println("Url is empty, skipping");
            return;
        }

        System.out.println("Crawling: " + url_to_crawl);

        extractProductData(doc);

        Elements links = doc.select("a.page-numbers");
        
        for(Element link : links){

            String current_url = link.attr("abs:href");
            if(current_url.isEmpty()){
                continue;
            }
            if (!current_url.substring(0,7).equals("http://") && !current_url.substring(0,8).equals("https://") ){
                continue;
            }
            if (!visited_urls.contains(current_url)){
                visited_urls.add(current_url);
                crawl(current_url, depth+1);
            }
        }

        return;
    }

    private static void exportDataToCsv(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Product Name, Price, image URL, product Listing\n");
            
            for (productRecord row : productData) {
                writer.append(row.toCsvRow());
                writer.append("\n");
            }

            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private static void exportDataToCsvByColor(String baseFilePath) {
        for (String color : colors) {
            String colorName = color.replace("%20", "_"); // Clean filename
            String filePath = baseFilePath.replace(".csv", "_" + colorName + ".csv");
            
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.append("Product Name,Price,Image URL,Product Listing\n");
                
                List<productRecord> colorProducts = productDataByColor.get(color);
                for (productRecord row : colorProducts) {
                    writer.append(row.toCsvRow());
                    writer.append("\n");
                }
                
                System.out.println("Data for " + colorName + " saved to " + filePath + 
                                 " (" + colorProducts.size() + " products)");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // -- TODO ---
    // private static void exportDataToExcelByColor(String filePath) {
    //     try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
    //         // Create a sheet for each color
    //         for (String color : colors) {
    //             String colorName = color.replace("%20or%20", " or ").replace("%20", " ");
    //             String sheetName = colorName.length() > 31 ? colorName.substring(0, 31) : colorName;
                
    //             Sheet sheet = workbook.createSheet(sheetName);
                
    //             // Create header row
    //             Row headerRow = sheet.createRow(0);
    //             headerRow.createCell(0).setCellValue("Product Name");
    //             headerRow.createCell(1).setCellValue("Price");
    //             headerRow.createCell(2).setCellValue("Image URL");
    //             headerRow.createCell(3).setCellValue("Product Listing");
                
    //             // Add data rows
    //             List<productRecord> colorProducts = productDataByColor.get(color);
    //             for (int i = 0; i < colorProducts.size(); i++) {
    //                 Row dataRow = sheet.createRow(i + 1);
    //                 productRecord product = colorProducts.get(i);
                    
    //                 dataRow.createCell(0).setCellValue(product.name);
    //                 dataRow.createCell(1).setCellValue(product.price);
    //                 dataRow.createCell(2).setCellValue(product.imageUrl);
    //                 dataRow.createCell(3).setCellValue(""); // Add product listing if available
    //             }
                
    //             // Auto-size columns
    //             for (int i = 0; i < 4; i++) {
    //                 sheet.autoSizeColumn(i);
    //             }
                
    //             System.out.println("Created sheet for " + colorName + " with " + 
    //                              colorProducts.size() + " products");
    //         }
            
    //         // Create summary sheet
    //         createSummarySheet(workbook);
            
    //         // Write to file
    //         try (FileOutputStream fileOut = new FileOutputStream(filePath.replace(".csv", ".xlsx"))) {
    //             workbook.write(fileOut);
    //             System.out.println("Excel file saved to " + filePath.replace(".csv", ".xlsx"));
    //         }
            
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }
    
    // private static void createSummarySheet(XSSFWorkbook workbook) {
    //     Sheet summarySheet = workbook.createSheet("Summary");
        
    //     // Header
    //     Row headerRow = summarySheet.createRow(0);
    //     headerRow.createCell(0).setCellValue("Color");
    //     headerRow.createCell(1).setCellValue("Product Count");
        
    //     // Data
    //     int rowNum = 1;
    //     int totalProducts = 0;
        
    //     for (String color : colors) {
    //         String colorName = color.replace("%20or%20", " or ").replace("%20", " ");
    //         int productCount = productDataByColor.get(color).size();
            
    //         Row dataRow = summarySheet.createRow(rowNum++);
    //         dataRow.createCell(0).setCellValue(colorName);
    //         dataRow.createCell(1).setCellValue(productCount);
            
    //         totalProducts += productCount;
    //     }
        
    //     // Total row
    //     Row totalRow = summarySheet.createRow(rowNum + 1);
    //     totalRow.createCell(0).setCellValue("TOTAL");
    //     totalRow.createCell(1).setCellValue(totalProducts);
        
    //     // Auto-size columns
    //     summarySheet.autoSizeColumn(0);
    //     summarySheet.autoSizeColumn(1);
    // }

}
