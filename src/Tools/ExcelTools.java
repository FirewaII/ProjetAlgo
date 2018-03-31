package Tools;

import models.Customer;
import models.Hub;
import models.Producer;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import models.Location;

public class ExcelTools {

    public static Location[] readLocations(File file, String sheetName) {
        // Initialization
        Location[] locations;
        XSSFSheet sheet = openExcelFile(file, sheetName);
        XSSFCell cell;
        XSSFRow row = sheet.getRow(0);
        int rows = sheet.getPhysicalNumberOfRows();
        int col = 0;
        int colNum = 0;
        int colName = -1;
        int colGPS1 = -1;
        int colGPS2 = -1;

        /* Get Column of each parameter */
        while (colGPS1 == -1 || colGPS2 == -1 || colName == -1) {
            cell = row.getCell(col);
            String cellValue = cell.getStringCellValue();
            if (cellValue.equals("Entreprise") || cellValue.equals("Client") || cellValue.equals("Ville")) {
                colName = col;
            } else if (cell.getStringCellValue().equals("CoordGPS1")) {
                colGPS1 = col;
            } else if (cell.getStringCellValue().equals("CoordGPS2")) {
                colGPS2 = col;
            }
            col++;
        }

        /* Full location array */
        // Initialization
        locations = new Location[rows - 1];

        // Read file
        for (int r = 1; r < rows; r++) {
            row = sheet.getRow(r);
            if (row != null) {
                // Get values of current row
                int noPlace = Integer.parseInt(row.getCell(colNum).getRawValue());
                String name = row.getCell(colName).getStringCellValue();
                double latitude = Double.parseDouble(row.getCell(colGPS1).getRawValue());
                double longitude = Double.parseDouble(row.getCell(colGPS2).getRawValue());

                // Put current location in Location array
                locations[r - 1] = new Location(noPlace, name, longitude, latitude);
            }
        }

        return locations;
    }

    public static Producer[] readProducers(File file) {
        // Initialisation
        Location[] locations = readLocations(file, "DonnéesFermes");
        Producer[] producers = new Producer[locations.length + 1];

        if (locations != null) {
            // Set fictionnal producer
            producers[0] = new Producer(0, "Fiction", 45.14429, 5.20811);

            // Set other Producers based on Location array
            for (int i = 0; i < locations.length; i++) {
                Location currentLoc = locations[i];
                producers[i + 1] = new Producer(currentLoc.getNoPlace(), currentLoc.getName(), currentLoc.getLongitude(), currentLoc.getLatitude());
            }

            /* Get producer supply */
            //Initialization
            int rNum = 1;
            int supplyQuantity;
            int col;
            XSSFSheet sheet = openExcelFile(file, "DonnéesProd");
            XSSFCell cell;
            XSSFRow row = sheet.getRow(rNum);
            HashSet<String> products = new HashSet<>(); // Set of products

            while (rNum != sheet.getPhysicalNumberOfRows()) {
                // Update Product Set if a new product is found. The purpose is to know the full list of products
                String product = row.getCell(1).getStringCellValue();
                if (!products.contains(product)) {
                    products.add(product);
                }

                /* Get Producer supply for current product for each period */
                // Initialization
                col = 2;
                cell = row.getCell(col);

                // Read current product(=line of file)
                while (sheet.getRow(0).getCell(col) != null && sheet.getRow(0).getCell(col).getStringCellValue().startsWith("Sem")) {
                    supplyQuantity = Integer.parseInt(cell.getRawValue());
                    Producer currentProducer = producers[Integer.parseInt(row.getCell(0).getRawValue())];
                    currentProducer.setSupply(col - 2, product, supplyQuantity); // col-2 = period number

                    // Go to the next column
                    col++;
                    cell = row.getCell(col);
                }
                // Go to the next line
                rNum++;
                row = sheet.getRow(rNum);
            }

            /* Set producer supply to 0 for each period if a producer does not provide a product */
            for (Producer producer : producers
                    ) {
                if (!producer.getName().equals("Fiction")) {
                    if (producer.getSupply().size() != products.size()) {
                        for (String product : products
                                ) {
                            for (int i = 0; i < producer.getSupply().size(); i++) {
                                if (!producer.getSupply().get(i).containsKey(product)) {
                                    producer.setSupply(i, product, 0);
                                }
                            }
                        }
                    }
                }

            }
        }
        return producers;
    }

    public static Customer[] readCustomers(File file) {
        // Initialization
        Location[] locations = readLocations(file, "DonnéesClients");
        Customer[] customers = new Customer[locations.length + 1];

        if (locations != null) {
            /* Read Customer category */
            // Initialization
            int colCategorie = -1;
            int col = 1;
            XSSFSheet sheet = openExcelFile(file, "DonnéesClients");
            XSSFRow row = sheet.getRow(0);
            XSSFCell cell;

            // Look for the column where category field is given
            while (colCategorie == -1) {
                cell = row.getCell(col);
                String cellValue = cell.getStringCellValue();
                if (cellValue.equals("Catégorie")) {
                    colCategorie = col;
                }
                col++;
            }

            // Set fictional customer
            customers[0] = new Customer(0, "Fiction", "Supermarché", 45.1934574, 5.7682659);

            // Set other customers based on Location array
            for (int i = 0; i < locations.length; i++) {
                Location currentLoc = locations[i];

                // Read category of current customer
                row = sheet.getRow(currentLoc.getNoPlace());
                cell = row.getCell(colCategorie);

                // Create and store current Customer in Customer array
                customers[i + 1] = new Customer(currentLoc.getNoPlace(), currentLoc.getName(), cell.getStringCellValue(), currentLoc.getLongitude(), currentLoc.getLatitude());
            }

            /* Get Customer demand */
            // Initialization
            int rNum = 1;
            int demandQuantity;
            sheet = openExcelFile(file, "DonnéesDemandes");
            row = sheet.getRow(rNum);
            HashSet<String> products = new HashSet<>(); // Set of products

            while (rNum != sheet.getPhysicalNumberOfRows()) {
                // Update Product Set if a new product is found. The purpose is to know the full list of products
                String product = row.getCell(1).getStringCellValue();
                if (!products.contains(product)) {
                    products.add(product);
                }

                /* Get Customer demand for current product for each period */
                // Initialization
                col = 2;
                cell = row.getCell(col);

                // Read current product(=line of file)
                while (sheet.getRow(0).getCell(col) != null && sheet.getRow(0).getCell(col).getStringCellValue().startsWith("Sem")) {
                    demandQuantity = Integer.parseInt(cell.getRawValue());
                    Customer currentCustomer = customers[Integer.parseInt(row.getCell(0).getRawValue())];
                    currentCustomer.setDemand(col - 2, product, demandQuantity); // col-2 = period number

                    // Go to the next column
                    col++;
                    cell = row.getCell(col);
                }
                // Go to the next line
                rNum++;
                row = sheet.getRow(rNum);
            }

            /* Set customer demand to 0 for each period if a customer does not want a product */
            for (Customer customer : customers
                    ) {
                if (!customer.getName().equals("Fiction")) {
                    if (customer.getDemand().size() != products.size()) {
                        for (String product : products
                                ) {
                            for (int i = 0; i < customer.getDemand().size(); i++) {
                                if (!customer.getDemand().get(i).containsKey(product)) {
                                    customer.setDemand(i, product, 0);
                                }
                            }
                        }
                    }
                }
            }

        }
        return customers;
    }

    public static Hub[] readHubs(File file) {
        // Initialization
        Location[] locations = readLocations(file, "Plateformes");
        Hub[] hubs = new Hub[locations.length];

        if (locations != null) {
            /* Read Hubs opCost */
            // Initialization
            XSSFSheet sheet = openExcelFile(file, "Plateformes");
            XSSFCell cell;
            XSSFRow row = sheet.getRow(0);
            int colOpCost = -1;
            int col = 1;

            // Get opCost column in excelFile
            while (colOpCost == -1) {
                cell = row.getCell(col);
                String cellValue = cell.getStringCellValue();
                if (cellValue.equals("Coût ouverture")) {
                    colOpCost = col;
                }
                col++;
            }

            // Set Hubs based on Location array
            for (int i = 0; i < locations.length; i++) {
                Location currentLoc = locations[i];

                //Read opCost of current hub
                row = sheet.getRow(currentLoc.getNoPlace());
                cell = row.getCell(colOpCost);

                // Create and store current Hub in Hub array
                hubs[i] = new Hub(currentLoc.getNoPlace(), currentLoc.getName(), Integer.parseInt(cell.getRawValue()), currentLoc.getLongitude(), currentLoc.getLatitude());
            }
        }
        return hubs;
    }

    public static double readShippingCost(File file, String request) {
        // Initialization
        double cost = 0;
        String from;
        String to;

        // Get origin of produdt
        if (request.startsWith("P")) { // P represents "Producer" other cases are for Hubs
            from = "Producteur";
        } else {
            from = "Plateforme";
        }
        // Get destination of product
        if (request.endsWith("C")) { // C represents "Customer" other cases are for hubs
            to = "Client";
        } else {
            to = "Plateforme";
        }

        /* Get Shipping cost in excelFile */
        XSSFSheet sheet = openExcelFile(file, "CoutsKM");
        XSSFRow row;
        int rNum = 1;
        boolean stop = false;
        while (!stop) {
            row = sheet.getRow(rNum);
            if (row.getCell(0).getStringCellValue().equals(from)) { // Check origin
                if (row.getCell(1).getStringCellValue().equals(to)) { // Check destination if origin is valid
                    cost = Double.parseDouble(row.getCell(2).getRawValue());
                    stop = true;
                }
            }
            rNum++;
        }

        return cost;
    }

    public static XSSFSheet openExcelFile(File excelFile, String sheetName) {
        try {
            FileInputStream fs = new FileInputStream(excelFile);
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            return wb.getSheet(sheetName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
