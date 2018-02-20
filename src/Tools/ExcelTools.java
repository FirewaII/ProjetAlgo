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
import java.util.HashMap;

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
                    double longitude = Double.parseDouble(row.getCell(colGPS1).getRawValue());
                    double latitude = Double.parseDouble(row.getCell(colGPS2).getRawValue());

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
        Producer[] producers = new Producer[locations.length];

        if(locations != null) {
            for (int i = 0; i < locations.length; i++) {
                Location currentLoc = locations[i];
                producers[i] = new Producer(currentLoc.getNoPlace(), currentLoc.getName(), currentLoc.getLongitude(), currentLoc.getLatitude(), new HashMap<>());
            }
        }


        try {
            FileInputStream fs = new FileInputStream(file);
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            XSSFRow row;
            XSSFCell cell;
            XSSFSheet sheet = wb.getSheet("DonnéesClients");

            /* Get Producer supplies */
            sheet = wb.getSheet("DonnéesProd");
            for (Producer producer : producers
                    ) {
                int rNum = ((producer.getNoPlace() - 1) * 3) + 1;
                row = sheet.getRow(rNum); // get first row of current user
                while (rNum != sheet.getPhysicalNumberOfRows() && Integer.parseInt(row.getCell(0).getRawValue()) == producer.getNoPlace()) {
                    String product = row.getCell(1).getStringCellValue();
                    int col = 2;
                    int supply = 0;
                    cell = row.getCell(col);
                    while (cell != null) {
                        supply += Integer.parseInt(cell.getRawValue());
                        col++;
                        cell = row.getCell(col);
                    }
                    producer.setSupply(product, supply);

                    rNum++;
                    row = sheet.getRow(rNum);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return producers;
    }

    public static Customer[] readCustomers(File file){
        Location[] locations = readLocations(file, "DonnéesClients");
        Customer[] customers = new Customer[locations.length];

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

                for (int i = 0; i < locations.length; i++) {
                    Location currentLoc = locations[i];

                    row = sheet.getRow(currentLoc.getNoPlace());
                    cell = row.getCell(colCategorie);

                    customers[i] = new Customer(currentLoc.getNoPlace(), currentLoc.getName(), cell.getStringCellValue(), currentLoc.getLongitude(), currentLoc.getLatitude(), new HashMap<>());
                }

                /* Get Customer demands */
                sheet = wb.getSheet("DonnéesDemandes");
                for (Customer customer: customers
                     ) {
                    int rNum = ((customer.getNoPlace() -1) *3)+1;
                    row = sheet.getRow(rNum); // get first row of current user
                    while(rNum != sheet.getPhysicalNumberOfRows() && Integer.parseInt(row.getCell(0).getRawValue()) == customer.getNoPlace()) {
                        String product = row.getCell(1).getStringCellValue();
                        col = 2;
                        int demand = 0;
                        cell = row.getCell(col);
                        while (cell != null){
                            demand += Integer.parseInt(cell.getRawValue());
                            col++;
                            cell = row.getCell(col);
                        }
                        customer.setDemand(product, demand);

                        rNum++;
                        row = sheet.getRow(rNum);
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
}
