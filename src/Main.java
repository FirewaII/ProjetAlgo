import Tools.ExcelTools;

import java.io.File;

public class Main {
    public static void main(String [] args){
        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx");
        ExcelTools.readExcelFile(excelFile);
    }
}
