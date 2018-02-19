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
import java.util.Map;
import java.util.Random;
import java.util.stream.*;

import static java.lang.Integer.max;

public class Main {
//    // Data
//    Boolean[] isOpen; // decides if a hub is open
//    int[] openCost; // opening cost of hubs
//    int[] offer; // producers offer set
//    int[] demand; // customers demand set
//
//    // Transport cost from location to location
//    float[][] costPtoH; // cost to transport products from Producer p to Hub h
//    float[][] costHtoC; // cost to transport products from Hub h to Customer c
//    float[][] costHtoH; // cost to transport products from Hub h to Hub h
//    float[][] costPtoC; // cost to transport products from Producer p to Customer c
//
//    // Number of products to transport (O/D)
//    int[][] nbPtoH; // Number of products to transport from Hub to Customer
//    int[][] nbHtoC; // Number of products to transport from Hub to Customer
//    int[][] nbHtoH; // Number of products to transport from Hub to Customer
//    int[][] nbPtoC; // Number of products to transport from Hub to Customer
//
//    int M = max(IntStream.of(offer).sum(), IntStream.of(demand).sum());


    public static void main(String[] args) throws Exception {
        /* Get Excel file */
        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx");

        /* Initialise variables */
//        Producer[] producers = ExcelTools.readProducers(excelFile);
//        Hub[] hubs = ExcelTools.readHubs(excelFile);
//        Customer[] customers = ExcelTools.readCustomers(excelFile);

        // Test Vars
        Random ran = new Random();
        int nbProduits = 3;
        double costPtoX = 1.0;
        double costHtoX = 0.5;

        // Sets
        Producer[] producers = {new Producer(1, "Ferme1", 45.14429, 5.20811),
                new Producer(2, "Ferme2", 45.71531, 5.67431),
                new Producer(3, "Ferme3", 45.52911, 5.73944)};

        for (Producer producer : producers) {
            producer.setSupply("Produits laitiers vache", ran.nextInt(50));
            producer.setSupply("Produits laitiers chèvre", ran.nextInt(70));
            producer.setSupply("Fruits", ran.nextInt(50));
        }

        Hub[] hubs = {new Hub(1, "Voiron", 17500, 45.35276, 5.56985),
                new Hub(2, "MIN de Grenoble", 20000, 45.17232, 5.71741)};

        int[] openCost = new int[hubs.length];
        int[] isOpen = new int[hubs.length];
        for (int i = 0; i < hubs.length; i++) {
            openCost[i] = hubs[i].getOpCost();
            isOpen[i] = 1;
        }

        Customer[] customers = {new Customer(1, "Client 1", "Supermarché", 45.17823, 5.74396),
                new Customer(2, "Client 2", "Supermarché", 45.4327231, 6.0192055),
                new Customer(3, "Client 3", "Supermarché", 45.1901677, 5.6940435),
                new Customer(4, "Client 4", "Supermarché", 45.5967377, 5.0944433),
                new Customer(5, "Client 5", "Supermarché", 45.6732628, 5.4846254)};

        for (Customer customer : customers) {
            customer.setDemand("Produits laitiers vache", ran.nextInt(30));
            customer.setDemand("Produits laitiers chèvre", ran.nextInt(40));
            customer.setDemand("Fruits", ran.nextInt(30));
        }


        // Offer / Demand
        int[][] offer = new int[producers.length][nbProduits];
        int[][] demand = new int[customers.length][nbProduits];


        for (int i = 0; i < producers.length; i++) {
            Map prodSupply = producers[i].getSupply();
            int j = 0;
            for (Object value : prodSupply.values()) {
                offer[i][j] = (Integer) value;
                j++;
            }
        }

        for (int i = 0; i < customers.length; i++) {
            Map custDemand = customers[i].getDemand();
            int j = 0;
            for (Object value : custDemand.values()) {
                demand[i][j] = (Integer) value;
                j++;
            }
        }

        int totalOffer = 0;
        for (int[] subOffer : offer) {
            totalOffer += IntStream.of(subOffer).sum();
        }

        int totalDemand = 0;
        for (int[] subDemand : demand) {
            totalDemand += IntStream.of(subDemand).sum();
        }


        // Big M
        int M = max(IntStream.of(totalOffer).sum(), IntStream.of(totalDemand).sum());


        // Distances

            // Prod => Hub
        int[][] dPH = new int[producers.length][hubs.length];
            // Hub => Cust
        int[][] dHC = new int[hubs.length][customers.length];
            // Prod => Cust
        int[][] dPC = new int[producers.length][customers.length];
            // Hub => Hub
        int[][] dHH = new int[hubs.length][hubs.length];

        System.out.println("Starting distance calculations");
        // Calcul des distances
        for (int i = 0; i < producers.length; i++) {
            for (int j = 0; j < hubs.length; j++) {
                dPH[i][j] = producers[i].getDistanceTo(hubs[j]);
                for (int h = 0; h < hubs.length; h++) {
                    dHH[j][h] = hubs[j].getDistanceTo(hubs[h]);
                }
                for (int k = 0; k < customers.length; k++) {
                    dHC[j][k] = hubs[j].getDistanceTo(customers[k]);
                }
            }
            for (int k = 0; k < customers.length; k++) {
                dPC[i][k] = producers[i].getDistanceTo(customers[k]);
            }
        }
        System.out.println("Distances calculated");

        // Transport cost

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