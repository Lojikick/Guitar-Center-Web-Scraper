package havocprod;

public class productRecord {
    String name;
    double price;
    String imageUrl;
    String listingUrl;

    public productRecord(String name, double price, String imageUrl, String listingUrl){
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.listingUrl = listingUrl;
    }

    public String toCsvRow() {
        return String.join(",", 
            escapeForCsv(name), 
            String.valueOf(price), 
            escapeForCsv(imageUrl),
            escapeForCsv(listingUrl)
        );
    }
    

    private String escapeForCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

}