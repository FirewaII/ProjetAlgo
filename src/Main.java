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
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import models.*;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.stream.*;
//import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra.*;
import static java.lang.Integer.max;
import static java.lang.Integer.sum;


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
        Producer[] producers = {new Producer(0, "Fiction", 0, 0),
                new Producer(1, "Ferme1", 45.14429, 5.20811),
                new Producer(2, "Ferme2", 45.71531, 5.67431),
                new Producer(3, "Ferme3", 45.52911, 5.73944)};

        for (Producer producer : producers) {
            if (producer.getName().equals("Fiction")) {
                continue;
            }
            producer.setSupply("Produits laitiers vache", ran.nextInt(50));
            producer.setSupply("Produits laitiers chèvre", ran.nextInt(70));
            producer.setSupply("Fruits", ran.nextInt(50));
        }

        Hub[] hubs = {new Hub(1, "Voiron", 17500, 45.35276, 5.56985),
                new Hub(2, "MIN de Grenoble", 20000, 45.17232, 5.71741)};

        double[][] openCost = new double[hubs.length][1];
        for (int i = 0; i < hubs.length; i++) {
            openCost[i][0] = (double) hubs[i].getOpCost();
        }

        Customer[] customers = {new Customer(0, "Fiction", "Supermarché", 0, 0),
                new Customer(1, "Client 1", "Supermarché", 45.17823, 5.74396),
                new Customer(2, "Client 2", "Supermarché", 45.4327231, 6.0192055),
                new Customer(3, "Client 3", "Supermarché", 45.1901677, 5.6940435),
                new Customer(4, "Client 4", "Supermarché", 45.5967377, 5.0944433),
                new Customer(5, "Client 5", "Supermarché", 45.6732628, 5.4846254)};

        for (Customer customer : customers) {
            if (customer.getName().equals("Fiction")) {
                continue;
            }
            customer.setDemand("Produits laitiers vache", ran.nextInt(30));
            customer.setDemand("Produits laitiers chèvre", ran.nextInt(40));
            customer.setDemand("Fruits", ran.nextInt(30));
        }


        // Offer / Demand
        double[][] offer = new double[producers.length][nbProduits];
        double[][] demand = new double[customers.length][nbProduits];


        for (int i = 0; i < producers.length; i++) {
            Map prodSupply = producers[i].getSupply();
            int j = 0;
            for (Object value : prodSupply.values()) {
                offer[i][j] = (double) (int) value;
                j++;
            }
        }

        for (int i = 0; i < customers.length; i++) {
            Map custDemand = customers[i].getDemand();
            int j = 0;
            for (Object value : custDemand.values()) {
                demand[i][j] = (double) (int) value;
                j++;
            }
        }

        int[] prodOffer = new int[nbProduits];
        for (double[] subOffer : offer) {
            prodOffer[0] += subOffer[0];
            prodOffer[1] += subOffer[1];
            prodOffer[2] += subOffer[2];
        }

        int[] prodDemand = new int[nbProduits];
        for (double[] subDemand : demand) {
            prodDemand[0] += subDemand[0];
            prodDemand[1] += subDemand[1];
            prodDemand[2] += subDemand[2];
        }

        // Big M
        int totalOffer = IntStream.of(prodOffer).sum();
        int totalDemand = IntStream.of(prodDemand).sum();
        int M = max(totalOffer, totalDemand);

        // Fictive offer/demand
        for (int i = 0; i < nbProduits; i++) {
            if (prodDemand[i] > prodOffer[i]) {
                producers[0].setSupply("Produit fictif " + i, prodDemand[i] - prodOffer[i]);
                customers[0].setDemand("Produit fictif " + i, 0);
            } else {
                customers[0].setDemand("Produit fictif " + i, prodOffer[i] - prodDemand[i]);
                producers[0].setSupply("Produit fictif " + i, 0);
            }
        }


        // Shipping cost using distances
        // Prod => Hub
        double[][] cPH = new double[producers.length][hubs.length];
        // Hub => Cust
        double[][] cHC = new double[hubs.length][customers.length];
        // Prod => Cust
        double[][] cPC = new double[producers.length][customers.length];
        // Hub => Hub
        double[][] cHH = new double[hubs.length][hubs.length];

        System.out.println("Starting shipping costs calculation...");
        // Calcul des couts de transport en fonction de la distance (km)
//        int coef;
//        for (int i = 0; i < producers.length; i++) {
//            if (producers[i].getName().contains("Fiction")) {
//                coef = 0;
//            } else {
//                coef = 1;
//            }
//            for (int j = 0; j < hubs.length; j++) {
//                cPH[i][j] = (producers[i].getDistanceTo(hubs[j]) / 1000) * costPtoX * coef;
//                for (int h = 0; h < hubs.length; h++) {
//                    cHH[j][h] = (hubs[j].getDistanceTo(hubs[h]) / 1000) * costHtoX;
//                }
//                for (int k = 0; k < customers.length; k++) {
//                    if (customers[i].getName().contains("Fiction")) {
//                        coef = 0;
//                    } else {
//                        coef = 1;
//                    }
//                    cHC[j][k] = (hubs[j].getDistanceTo(customers[k]) / 1000) * costHtoX * coef;
//                }
//            }
//            for (int k = 0; k < customers.length; k++) {
//                cPC[i][k] = (producers[i].getDistanceTo(customers[k]) / 1000) * costPtoX * coef;
//            }
//        }
        cPH = new double[][]{{0.0, 56.0, 67.0}, {0.0, 49.0, 95.0}, {0.0, 30.0, 57.0}};
        cHC = new double[][]{{0.0, 13.5, 28.5, 11.5, 30.5, 21.5}, {0.0, 1.0, 23.0, 2.0, 43.0, 42.5}};
        cPC = new double[][]{{0.0, 66.0, 109.0, 62.0, 68.0, 82.0}, {0.0, 88.0, 55.0, 98.0, 61.0, 22.0}, {0.0, 52.0, 46.0, 52.0, 61.0, 29.0}};
        cHH = new double[][]{{0.0, 14.0}, {13.5, 0.0}};
        System.out.println("Costs calculated");


        int N = 1; // number of elements in each set

        /* Create the optimization problem object */
        OptimizationProblem op = new OptimizationProblem();

        /* Add the decision variables to the problem */
        op.addDecisionVariable("isOpen", true, new int[]{2, 1}, 0, 1);  // name, isInteger, size , minValue, maxValue
        op.addDecisionVariable("yPH", true, new int[]{producers.length, hubs.length});
        op.addDecisionVariable("yHC", true, new int[]{hubs.length, customers.length});
        op.addDecisionVariable("yPC", true, new int[]{producers.length, customers.length});
        op.addDecisionVariable("yHH", true, new int[]{hubs.length, hubs.length});


        /* Set value for the input parameters */
        op.setInputParameter("openCost", openCost);
        op.setInputParameter("offer", new DoubleMatrixND(offer));
        op.setInputParameter("demand", new DoubleMatrixND(demand));
//
        op.setInputParameter("cPH", new DoubleMatrixND(cPH));
        op.setInputParameter("cHC", new DoubleMatrixND(cHC));
        op.setInputParameter("cPC", new DoubleMatrixND(cPC));
        op.setInputParameter("cHH", new DoubleMatrixND(cHH));


        /* Add the constraints */
        op.addConstraint("sum(sum(yPH,2),1) >= 0");
        op.addConstraint("sum(sum(yPC,2),1) >= 0");
        op.addConstraint("sum(sum(yHC,2),1) >= 0");
        op.addConstraint("sum(sum(yHH,2),1) >= 0");

        // Produits sortants - produits entrants ( H -> H + H -> C - P -> Hub )
        op.addConstraint("sum(sum(yPH,2),1) + sum(sum(yPC,2)1) - sum(offer,2) == 0");
        op.addConstraint("sum(sum(yHH,2),1) + sum(sum(yHC,2)1) - sum(sum(yPH,2),1) - sum(sum(yHH,2),1) == 0");
        op.addConstraint("sum(demand,2) - sum(sum(yPC,2),1) + sum(sum(yHC,2),1) == 0");

        /* Sets the objective function */
//        op.setObjectiveFunction("minimize", "sum(isOpen .* openCost) + sum(cPH .* yPH) + sum(cHC .* yHC) + sum(cPC .* yPC) + sum(cHH .* yHH)");
        op.setObjectiveFunction("minimize", " sum(cPH .* yPH)");


        /* Call the solver to solve the problem */
        op.solve("glpk");
        if (!op.solutionIsOptimal()) throw new RuntimeException("An optimal solution was not found");

        /* Print the solution */
        System.out.println(op.getPrimalSolution("isOpen"));
//        System.out.println(op.getPrimalSolution("cPH"));
//        System.out.println(op.getPrimalSolution("cHC"));
//        System.out.println(op.getPrimalSolution("cPC"));
//        System.out.println(op.getPrimalSolution("cHH"));

    }
}