import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates the plain-text input files used by the sales processing program.
 * This class is the first of the two executable classes required by the project.
 */
public class GenerateInfoFiles {

    private static final String[] DOCUMENT_TYPES = {"CC", "CE", "PAS"};
    private static final String[] FIRST_NAMES = {
        "Carlos", "Ana", "Juan", "Maria", "Luis", "Laura", "Diego", "Sofia", "Andres", "Valentina"
    };
    private static final String[] LAST_NAMES = {
        "Perez", "Gomez", "Rodriguez", "Martinez", "Lopez", "Garcia", "Hernandez", "Torres", "Vargas", "Rios"
    };
    private static final String[] PRODUCT_CATALOG = {
        "Notebook", "Pen", "Pencil", "Eraser", "Ruler", "Sharpener", "Marker", "Folder", "Scissors", "Glue"
    };

    private static final Random RANDOM = new Random();
    private static final List<SalesmanData> GENERATED_SALESMEN = new ArrayList<SalesmanData>();
    private static int availableProductCount = 10;

    /**
     * Generates products, salesmen and individual sales files without asking the user for data.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            int salesmanCount = 5;
            int productsCount = 10;
            int maxSalesPerSalesman = 8;

            deleteOldSalesFiles();
            createProductsFile(productsCount);
            createSalesManInfoFile(salesmanCount);

            for (SalesmanData salesman : GENERATED_SALESMEN) {
                int randomSalesCount = RANDOM.nextInt(maxSalesPerSalesman) + 1;
                createSalesMenFile(randomSalesCount, salesman.getFirstName(), salesman.getDocumentNumber());
            }

            System.out.println("Process completed successfully. Test files were generated.");
        } catch (Exception e) {
            System.err.println("Error generating test files: " + e.getMessage());
        }
    }

    /**
     * Creates the file with pseudo-random salesmen information.
     * The generated records are kept in memory so that the sales files use exactly the same people.
     *
     * @param salesmanCount number of salesmen to create
     * @throws IOException if the output file cannot be written
     */
    public static void createSalesManInfoFile(int salesmanCount) throws IOException {
        GENERATED_SALESMEN.clear();
        File file = new File("salesmen_info.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < salesmanCount; i++) {
                String docType = DOCUMENT_TYPES[RANDOM.nextInt(DOCUMENT_TYPES.length)];
                long docNumber = generateUniqueDocumentNumber();
                String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];

                SalesmanData salesman = new SalesmanData(docType, docNumber, firstName, lastName);
                GENERATED_SALESMEN.add(salesman);

                writer.write(docType + ";" + docNumber + ";" + firstName + ";" + lastName);
                writer.newLine();
            }
        }
    }

    /**
     * Creates a pseudo-random product catalog.
     *
     * @param productsCount number of products to create
     * @throws IOException if the output file cannot be written
     */
    public static void createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) {
            throw new IllegalArgumentException("The number of products must be greater than zero.");
        }

        availableProductCount = productsCount;
        File file = new File("products_info.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 1; i <= productsCount; i++) {
                String productId = "PROD" + String.format("%03d", i);
                String productName = PRODUCT_CATALOG[(i - 1) % PRODUCT_CATALOG.length] + " " + i;
                long unitPrice = (RANDOM.nextInt(50) + 1) * 1000L;

                writer.write(productId + ";" + productName + ";" + unitPrice);
                writer.newLine();
            }
        }
    }

    /**
     * Creates a pseudo-random sales file for a salesman.
     * Required project signature: createSalesMenFile(int, String, long).
     *
     * @param randomSalesCount number of sales detail rows
     * @param name first name of the salesman
     * @param id identification number of the salesman
     * @throws IOException if the output file cannot be written
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id) throws IOException {
        if (randomSalesCount < 0) {
            throw new IllegalArgumentException("The number of sales cannot be negative.");
        }

        SalesmanData salesman = findSalesman(id);
        String documentType = salesman == null ? "CC" : salesman.getDocumentType();
        File file = new File("sales_" + name + "_" + id + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(documentType + ";" + id);
            writer.newLine();

            for (int i = 0; i < randomSalesCount; i++) {
                int randomProductId = RANDOM.nextInt(availableProductCount) + 1;
                String productId = "PROD" + String.format("%03d", randomProductId);
                int quantitySold = RANDOM.nextInt(15) + 1;

                writer.write(productId + ";" + quantitySold + ";");
                writer.newLine();
            }
        }
    }

    private static long generateUniqueDocumentNumber() {
        long candidate;
        do {
            candidate = 1000000000L + RANDOM.nextInt(900000000);
        } while (findSalesman(candidate) != null);
        return candidate;
    }

    private static SalesmanData findSalesman(long id) {
        for (SalesmanData salesman : GENERATED_SALESMEN) {
            if (salesman.getDocumentNumber() == id) {
                return salesman;
            }
        }
        return null;
    }

    private static void deleteOldSalesFiles() {
        File currentDirectory = new File(".");
        File[] oldFiles = currentDirectory.listFiles((dir, name) -> name.startsWith("sales_") && name.endsWith(".txt"));
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                if (!oldFile.delete()) {
                    System.err.println("Warning: could not delete " + oldFile.getName());
                }
            }
        }
    }

    /** Holds a generated salesman record to preserve relational integrity. */
    private static class SalesmanData {
        private final String documentType;
        private final long documentNumber;
        private final String firstName;
        private final String lastName;

        SalesmanData(String documentType, long documentNumber, String firstName, String lastName) {
            this.documentType = documentType;
            this.documentNumber = documentNumber;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        String getDocumentType() { return documentType; }
        long getDocumentNumber() { return documentNumber; }
        String getFirstName() { return firstName; }
        @SuppressWarnings("unused")
        String getLastName() { return lastName; }
    }
}
