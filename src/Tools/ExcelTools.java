package Tools;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;

import models.Location;

public class ExcelTools {
    public static void readExcelFile(File file) {
        try {
            FileInputStream fs = new FileInputStream(file);
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            XSSFRow row;
            XSSFCell cell;

            int nbSheets;
            nbSheets = wb.getNumberOfSheets();

            for(int j = 0 ; j < nbSheets ; j++) {

                XSSFSheet sheet = wb.getSheetAt(j);

                int rows; // No of rows
                rows = sheet.getPhysicalNumberOfRows();

                int cols = 0; // No of columns
                int tmp;

                // This trick ensures that we get the data properly even if it doesn't start from first few rows
                for (int i = 0; i < 10 || i < rows; i++) {
                    row = sheet.getRow(i);
                    if (row != null) {
                        tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                        if (tmp > cols) cols = tmp;
                    }
                }

                for (int r = 0; r < rows; r++) {
                    row = sheet.getRow(r);
                    if (row != null) {
                        for (int c = 0; c < cols; c++) {
                            cell = row.getCell((short) c);
                            if (cell != null) {
                                System.out.println(cell.getRawValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    public static void readProducers(File file){
        try {
            FileInputStream fs = new FileInputStream(file);
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            XSSFRow row;
            XSSFCell cell;

            XSSFSheet sheet = wb.getSheet("DonnéesProd");

            int rows; // No of rows
            rows = sheet.getPhysicalNumberOfRows();

            int cols = 0; // No of columns
            int tmp;

            for (int r = 0; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    for (int c = 0; c < cols; c++) {
                        cell = row.getCell((short) c);
                        if (cell != null) {
                            System.out.println(cell.getRawValue());
                        }
                    }
                }
            }
        } catch (Exception ioe){
            ioe.printStackTrace();
        }
    }

    public static void readCustomers(){

    }

    public static void readHubs(){
        
    }
}
