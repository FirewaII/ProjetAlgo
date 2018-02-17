//import Tools.ExcelTools;
//
//import java.io.File;
//
//public class Main {
//    public static void main(String [] args){
//        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx");
//        ExcelTools.readExcelFile(excelFile);
//    }
//}

import Tools.ExcelTools;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import models.*;

import java.io.File;
import java.util.stream.*;

import static java.lang.Integer.max;

public class Main {
    // Data
    Boolean[] isOpen; // decides if a hub is open
    int[] openCost; // opening cost of hubs
    int[] offer; // producers offer set
    int[] demand; // customers demand set

    // Transport cost from location to location
    float[][] costPtoH; // cost to transport products from Producer p to Hub h
    float[][] costHtoC; // cost to transport products from Hub h to Customer c
    float[][] costHtoH; // cost to transport products from Hub h to Hub h
    float[][] costPtoC; // cost to transport products from Producer p to Customer c

    // Number of products to transport (O/D)
    int[][] nbPtoH; // Number of products to transport from Hub to Customer
    int[][] nbHtoC; // Number of products to transport from Hub to Customer
    int[][] nbHtoH; // Number of products to transport from Hub to Customer
    int[][] nbPtoC; // Number of products to transport from Hub to Customer

    int M = max(IntStream.of(offer).sum(), IntStream.of(demand).sum());


    public static void main(String[] args) {
        /* Get Excel file */
        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx");

        /* Initialise variables */
        Producer[] producers = ExcelTools.readProducers(excelFile);
        Hub[] hubs = ExcelTools.readHubs(excelFile);
        Customer[] customers = ExcelTools.readCustomers(excelFile);

        int N = 5; // number of elements in each set

        /* Create the optimization problem object */
        OptimizationProblem op = new OptimizationProblem();

        /* Add the decision variables to the problem */
        op.addDecisionVariable("x", true, new int[]{N, N, N}, 0, 1);  // name, isInteger, size , minValue, maxValue

        /* Set value for the input parameter c_{ijk} */
        op.setInputParameter("c", new DoubleMatrixND(new int[]{N, N, N}, "random"));

        /* Sets the objective function */
        op.setObjectiveFunction("maximize", "sum(x .* c)");

        /* Add the constraints */
        op.addConstraint(" sum(sum(x,3),2) <= 1");  // for each i \sum_{jk} x_{ijk} <= 1
        op.addConstraint(" sum(sum(x,3),1) <= 1");  // for each j \sum_{ik} x_{ijk} <= 1
        op.addConstraint(" sum(sum(x,2),1) <= 1");  // for each k \sum_{ij} x_{ijk} <= 1

        /* Call the solver to solve the problem */
        op.solve("glpk", "solverLibraryName", "glpk");
        if (!op.solutionIsOptimal()) throw new RuntimeException("An optimal solution was not found");

        /* Print the solution */
        DoubleMatrixND sol = op.getPrimalSolution("x");
        for (int c1 = 0; c1 < N; c1++)
            for (int c2 = 0; c2 < N; c2++)
                for (int c3 = 0; c3 < N; c3++)
                    if (sol.get(new int[]{c1, c2, c3}) == 1)
                        System.out.println(c1 + " - " + c2 + " - " + c3);
    }
}