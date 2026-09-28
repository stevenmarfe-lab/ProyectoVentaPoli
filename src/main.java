import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the generated input files, consolidates sales and creates the two CSV reports
 * required by the project. This is the second executable class of the project.
 */
public class main {

    private static final String SALESMEN_FILE = "salesmen_info.txt";
    private static final String PRODUCTS_FILE = "products_info.txt";
    private static final String SALESMEN_REPORT = "salesmen_report.csv";
    private static final String PRODUCTS_REPORT = "products_report.csv";

    /**
     * Executes the complete sales-processing flow without requesting information from the user.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            Map<String, Salesman> salesmen = readSalesmen(SALESMEN_FILE);
            Map<String, Product> products = readProducts(PRODUCTS_FILE);
            processSalesFiles(salesmen, products);
            writeSalesmenReport(salesmen);
            writeProductsReport(products);
            System.out.println("Process completed successfully. CSV reports were generated.");
        } catch (Exception e) {
            System.err.println("Error processing sales: " + e.getMessage());
        }
    }

    private static Map<String, Salesman> readSalesmen(String fileName) throws IOException {
        Map<String, Salesman> salesmen = new HashMap<String, Salesman>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(";");
                if (parts.length != 4) {
                    throw new IOException("Invalid format in " + fileName + ", line " + lineNumber + ".");
                }
                String key = salesmanKey(parts[0], parts[1]);
                salesmen.put(key, new Salesman(parts[0], parts[1], parts[2], parts[3]));
            }
        }
        return salesmen;
    }

    private static Map<String, Product> readProducts(String fileName) throws IOException {
        Map<String, Product> products = new HashMap<String, Product>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(";");
                if (parts.length != 3) {
                    throw new IOException("Invalid format in " + fileName + ", line " + lineNumber + ".");
                }
                long price = parseNonNegativeLong(parts[2], "price", fileName, lineNumber);
                products.put(parts[0], new Product(parts[0], parts[1], price));
            }
        }
        return products;
    }

    private static void processSalesFiles(Map<String, Salesman> salesmen, Map<String, Product> products)
            throws IOException {
        File directory = new File(".");
        File[] salesFiles = directory.listFiles((dir, name) -> name.startsWith("sales_") && name.endsWith(".txt"));

        if (salesFiles == null || salesFiles.length == 0) {
            throw new IOException("No sales files were found in the project folder.");
        }

        for (File salesFile : salesFiles) {
            processOneSalesFile(salesFile, salesmen, products);
        }
    }

    private static void processOneSalesFile(File salesFile, Map<String, Salesman> salesmen,
            Map<String, Product> products) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(salesFile))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IOException("The file " + salesFile.getName() + " is empty.");
            }

            String[] sellerData = header.split(";");
            if (sellerData.length != 2) {
                throw new IOException("Invalid header in " + salesFile.getName() + ".");
            }

            Salesman salesman = salesmen.get(salesmanKey(sellerData[0], sellerData[1]));
            if (salesman == null) {
                throw new IOException("The salesman " + header + " from " + salesFile.getName()
                        + " does not exist in " + SALESMEN_FILE + ".");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(";", -1);
                if (parts.length < 2 || parts.length > 3) {
                    throw new IOException("Invalid format in " + salesFile.getName() + ", line " + lineNumber + ".");
                }

                Product product = products.get(parts[0]);
                if (product == null) {
                    throw new IOException("The product " + parts[0] + " from " + salesFile.getName()
                            + " does not exist in " + PRODUCTS_FILE + ".");
                }

                long quantity = parseNonNegativeLong(parts[1], "quantity", salesFile.getName(), lineNumber);
                product.quantitySold += quantity;
                salesman.moneyCollected += quantity * product.unitPrice;
            }
        }
    }

    private static void writeSalesmenReport(Map<String, Salesman> salesmen) throws IOException {
        List<Salesman> ordered = new ArrayList<Salesman>(salesmen.values());
        Collections.sort(ordered, new Comparator<Salesman>() {
            @Override
            public int compare(Salesman a, Salesman b) {
                return Long.compare(b.moneyCollected, a.moneyCollected);
            }
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SALESMEN_REPORT))) {
            writer.write("Salesman;MoneyCollected");
            writer.newLine();
            for (Salesman salesman : ordered) {
                writer.write(salesman.firstName + " " + salesman.lastName + ";" + salesman.moneyCollected);
                writer.newLine();
            }
        }
    }

    private static void writeProductsReport(Map<String, Product> products) throws IOException {
        List<Product> ordered = new ArrayList<Product>(products.values());
        Collections.sort(ordered, new Comparator<Product>() {
            @Override
            public int compare(Product a, Product b) {
                return Long.compare(b.quantitySold, a.quantitySold);
            }
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRODUCTS_REPORT))) {
            writer.write("Product;Price;QuantitySold");
            writer.newLine();
            for (Product product : ordered) {
                writer.write(product.name + ";" + product.unitPrice + ";" + product.quantitySold);
                writer.newLine();
            }
        }
    }

    private static long parseNonNegativeLong(String value, String field, String fileName, int lineNumber)
            throws IOException {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 0) {
                throw new IOException("Negative value for " + field + " en " + fileName + ", line " + lineNumber + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IOException("Non-numeric value for " + field + " en " + fileName + ", line " + lineNumber + ".");
        }
    }

    private static String salesmanKey(String documentType, String documentNumber) {
        return documentType.trim() + ";" + documentNumber.trim();
    }

    private static class Salesman {
        @SuppressWarnings("unused")
        private final String documentType;
        @SuppressWarnings("unused")
        private final String documentNumber;
        private final String firstName;
        private final String lastName;
        private long moneyCollected;

        Salesman(String documentType, String documentNumber, String firstName, String lastName) {
            this.documentType = documentType;
            this.documentNumber = documentNumber;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    private static class Product {
        @SuppressWarnings("unused")
        private final String id;
        private final String name;
        private final long unitPrice;
        private long quantitySold;

        Product(String id, String name, long unitPrice) {
            this.id = id;
            this.name = name;
            this.unitPrice = unitPrice;
        }
    }
}
