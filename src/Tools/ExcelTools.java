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

    public static Location[] readLocations(File file, String sheetName){
        /* Generate an array of Locations */
        Location[] locations = null;
        try {
            FileInputStream fs = new FileInputStream(file);
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            XSSFRow row;
            XSSFCell cell;

            XSSFSheet sheet = wb.getSheet(sheetName);

            int rows = sheet.getPhysicalNumberOfRows(); // Number of rows
            int col = 0;
            int colNum = 0;
            int colName = -1;
            int colGPS1 = -1;
            int colGPS2 = -1;
            row = sheet.getRow(0);


            /* Get Columns of parameters */
            while (colGPS1 == -1 || colGPS2 == -1 || colName == -1){
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
            locations = new Location[rows-1];

            for (int r = 1 ; r < rows ; r++) {
                row = sheet.getRow(r);
                if (row != null){
                    /* Get values of current row */
                    int noPlace = Integer.parseInt(row.getCell(colNum).getRawValue());
                    String name = row.getCell(colName).getStringCellValue();
                    double latitude = Double.parseDouble(row.getCell(colGPS1).getRawValue());
                    double longitude = Double.parseDouble(row.getCell(colGPS2).getRawValue());

                    locations[r-1] = new Location(noPlace, name, longitude, latitude);
                }
            }
        } catch (Exception ioe){
            ioe.printStackTrace();
        }

        return locations;
    }

    public static Producer[] readProducers(File file){
        Location[] locations = readLocations(file, "DonnéesFermes");
        Producer[] producers = new Producer[locations.length + 1];

            if(locations != null) {
                try {
                    // Set fictionnal producer
                    producers[0] = new Producer(0, "Fiction", 45.14429, 5.20811);
                    for (int i = 0; i < locations.length; i++) {
                        Location currentLoc = locations[i];
                        producers[i + 1] = new Producer(currentLoc.getNoPlace(), currentLoc.getName(), currentLoc.getLongitude(), currentLoc.getLatitude());
                    }

                    FileInputStream fs = new FileInputStream(file);
                    XSSFWorkbook wb = new XSSFWorkbook(fs);
                    XSSFRow row;
                    XSSFCell cell;
                    XSSFSheet sheet = wb.getSheet("DonnéesProd");

                    /* Get producer supplies */
                    int rNum = 1;
                    int supplyQuantity;
                    int col;
                    row = sheet.getRow(rNum);
                    HashSet<String> products = new HashSet<>(); // Set of products

                    while (rNum != sheet.getPhysicalNumberOfRows()) {
                        String product = row.getCell(1).getStringCellValue();
                        if (!products.contains(product)) {
                            products.add(product);
                        }

                        col = 2;
                        cell = row.getCell(col);
                        while (sheet.getRow(0).getCell(col) != null && sheet.getRow(0).getCell(col).getStringCellValue().startsWith("Sem")) {
                            supplyQuantity = Integer.parseInt(cell.getRawValue());
                            Producer currentProducer = producers[Integer.parseInt(row.getCell(0).getRawValue())];
                            currentProducer.setSupply(col-2, product, supplyQuantity);
                            col++;
                            cell = row.getCell(col);
                        }

                        rNum++;
                        row = sheet.getRow(rNum);
                    }

                    for (Producer producer: producers
                         ) {
                        if(!producer.getName().equals("Fiction")) {
                            if (producer.getSupply().size() != products.size()) {
                                for (String product : products
                                        ) {
                                    for (int i = 0; i < producer.getSupply().size() ; i++){
                                        if (!producer.getSupply().get(i).containsKey(product)) {
                                            producer.setSupply(i, product, 0);
                                        }
                                    }
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        return producers;
    }

    public static Customer[] readCustomers(File file){
        Location[] locations = readLocations(file, "DonnéesClients");
        Customer[] customers = new Customer[locations.length + 1];

        if (locations != null){
            try {
                FileInputStream fs = new FileInputStream(file);
                XSSFWorkbook wb = new XSSFWorkbook(fs);
                XSSFRow row;
                XSSFCell cell;
                XSSFSheet sheet = wb.getSheet("DonnéesClients");
                row = sheet.getRow(0);

                int colCategorie = -1;
                int col = 1;
                while (colCategorie == -1 ) {
                    cell = row.getCell(col);
                    String cellValue = cell.getStringCellValue();
                    if (cellValue.equals("Catégorie")) {
                        colCategorie = col;
                    }
                    col++;
                }

                if (locations != null){
                    // Set fictional customer
                    customers[0] = new Customer(0, "Fiction", "Supermarché", 45.1934574, 5.7682659);
                    for (int i = 0; i < locations.length; i++) {
                        Location currentLoc = locations[i];
                        row = sheet.getRow(currentLoc.getNoPlace());
                        cell = row.getCell(colCategorie);

                        customers[i + 1] = new Customer(currentLoc.getNoPlace(), currentLoc.getName(), cell.getStringCellValue(), currentLoc.getLongitude(), currentLoc.getLatitude());
                    }
                }

                sheet = wb.getSheet("DonnéesDemandes");

                /* Get Customer demands */
                int rNum = 1;
                int demandQuantity;
                row = sheet.getRow(rNum);
                HashSet<String > products = new HashSet<>(); // Set of products

                while (rNum != sheet.getPhysicalNumberOfRows()){
                    String product = row.getCell(1).getStringCellValue();
                    if(!products.contains(product)){
                        products.add(product);
                    }

                    col = 2;
                    cell = row.getCell(col);
                    while (sheet.getRow(0).getCell(col) != null && sheet.getRow(0).getCell(col).getStringCellValue().startsWith("Sem")){
                        demandQuantity = Integer.parseInt(cell.getRawValue());
                        Customer currentCustomer = customers[Integer.parseInt(row.getCell(0).getRawValue())];
                        currentCustomer.setDemand(col-2, product, demandQuantity);
                        col++;
                        cell = row.getCell(col);
                    }

                    rNum++;
                    row = sheet.getRow(rNum);
                }

                for (Customer customer: customers
                        ) {
                    if (!customer.getName().equals("Fiction")) {
                        if (customer.getDemand().size() != products.size()) {
                            for (String product : products
                                    ) {
                                for (int i = 0; i < customer.getDemand().size() ; i++){
                                    if (!customer.getDemand().get(i).containsKey(product)) {
                                        customer.setDemand(i, product, 0);
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return customers;
    }

    public static Hub[] readHubs(File file){
        Location[] locations = readLocations(file, "Plateformes");
        Hub[] hubs = new Hub[locations.length];

        if(locations != null) {

            try {
                FileInputStream fs = new FileInputStream(file);
                XSSFWorkbook wb = new XSSFWorkbook(fs);
                XSSFRow row;
                XSSFCell cell;
                XSSFSheet sheet = wb.getSheet("Plateformes");

                row = sheet.getRow(0);
                int colOpCost = -1;
                int col = 1;
                while (colOpCost == -1 ) {
                    cell = row.getCell(col);
                    String cellValue = cell.getStringCellValue();
                    if (cellValue.equals("Coût ouverture")) {
                        colOpCost = col;
                    }
                    col++;
                }
                for (int i = 0; i < locations.length; i++) {
                    Location currentLoc = locations[i];

                    row = sheet.getRow(currentLoc.getNoPlace());
                    cell = row.getCell(colOpCost);

                    hubs[i] = new Hub(currentLoc.getNoPlace(), currentLoc.getName(), Integer.parseInt(cell.getRawValue()), currentLoc.getLongitude(), currentLoc.getLatitude());
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return hubs;
    }

    public static double readCost(File file, String request){
        double cost = 0;
        String from;
        String to;

        if(request.startsWith("P")){ // P represents "Producer" other cases are for Hubs
            from = "Producteur";
        } else {
            from = "Plateforme";
        }

        if(request.endsWith("C")){ // C represents "Customer" other cases are for hubs
            to = "Client";
        } else {
            to = "Plateforme";
        }
        try {
            FileInputStream fs = new FileInputStream(file);
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            XSSFRow row;
            XSSFSheet sheet = wb.getSheet("CoutsKM");

            int rNum = 1;
            boolean stop = false;
            while (!stop){
                row = sheet.getRow(rNum);
                if(row.getCell(0).getStringCellValue().equals(from)){ // Check origin
                    if(row.getCell(1).getStringCellValue().equals(to)){ // Check destination if origin is valid
                        cost = Double.parseDouble(row.getCell(2).getRawValue());
                        stop = true;
                    }
                }
                rNum++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cost;
    }
}
