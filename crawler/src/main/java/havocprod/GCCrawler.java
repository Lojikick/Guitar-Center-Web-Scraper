package havocprod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GCCrawler {
    private static ArrayList<String> urls = new ArrayList<String>();
    private static Set<String> visited_urls = new HashSet<String>();
    private static List<productRecord> productData = new ArrayList<>();
    private static int maxDepth = 5;


    public static void main(String[] args){
        String seedUrl = "https://www.guitarcenter.com/Solid-Body-Electric-Guitars.gc";

        
        visited_urls.add(seedUrl);
        urls.add(seedUrl);
        
        crawlSingle(seedUrl, 0);
        for (String curr_url : visited_urls){
            System.out.println(curr_url);
        }
        
        exportDataToCsv("guitar_center_product_data.csv");

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

        System.out.println("Crawling: " + url_to_crawl);
    
        extractProductData(doc);

        return;
    }

    private static void extractProductData(Document document){
        
        Elements products = document.select("section.plp-product-grid");
        
        if (products == null){
            System.err.println("Unable to fetch product elem");
            return;
        }
        for(Element product : products){
            //Implement error checking oh god
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

            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .timeout(10000) // 10 second timeout
                .get();

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
}
