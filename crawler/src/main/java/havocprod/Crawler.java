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


public class Crawler {

    private static ArrayList<String> urls = new ArrayList<String>();
    private static Set<String> visited_urls = new HashSet<String>();
    private static List<String[]> productData = new ArrayList<>();
    private static int maxDepth = 5;


    public static void main(String[] args){
        String seedUrl = "https://www.scrapingcourse.com/ecommerce/";

        
        visited_urls.add(seedUrl);
        urls.add(seedUrl);
        
        crawl(seedUrl, 0);
        for (String curr_url : visited_urls){
            System.out.println(curr_url);
        }

        exportDataToCsv("test_product_data.csv");
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


    private static void extractProductData(Document document){

        Elements products = document.select("li.product");

        for (Element product : products){
            String productName = product.select(".product-name").text();
            String price = product.select(".product-price").text();
            String imageUrl = product.select(".product-image").attr("src");

            System.out.println("product-name: " + productName);
            System.out.println("product-name: " + price);
            System.out.println("product-image: " + imageUrl);

            productData.add(new String[]{productName, price, imageUrl});
        }

    }

    private static void exportDataToCsv(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Product Name, Price, image URL\n");
            
            for (String[] row : productData) {
                writer.append(String.join(",", row));
                writer.append("\n");
            }

            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
