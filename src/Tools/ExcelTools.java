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


            /* Get Columns of latitude and longitude */
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
                producers[i] = new Producer(currentLoc.getNoPlace(), currentLoc.getName(), currentLoc.getLongitude(), currentLoc.getLatitude(), null);
            }
        }
        return producers;
    }

    public static Customer[] readCustomers(File file){
        Location[] locations = readLocations(file, "DonnéesClients");
        Customer[] customers = new Customer[locations.length];

        if (locations != null){
            for (int i = 0; i < locations.length; i++) {
                Location currentLoc = locations[i];
                customers[i] = new Customer(currentLoc.getNoPlace(), currentLoc.getName(), null, currentLoc.getLongitude(), currentLoc.getLatitude(), null);
            }
        }
        return customers;
    }

    public static Hub[] readHubs(File file){
        Location[] locations = readLocations(file, "DonnéesFermes");
        Hub[] hubs = new Hub[locations.length];

        if(locations != null) {
            for (int i = 0; i < locations.length; i++) {
                Location currentLoc = locations[i];
                hubs[i] = new Hub(currentLoc.getNoPlace(), currentLoc.getName(), -1, currentLoc.getLongitude(), currentLoc.getLatitude());
            }
        }
        return hubs;
    }
}
