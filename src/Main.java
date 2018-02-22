
import Tools.ExcelTools;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import models.*;

import java.io.File;
import java.util.Map;
import java.util.stream.*;
import static java.lang.Integer.max;


public class Main {

    public static void main(String[] args) throws Exception {
        /* Get Excel file */
        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx");

        System.out.println("Adding locations...");
        /* Initialise variables */
        Producer[] producers = ExcelTools.readProducers(excelFile);
        Hub[] hubs = ExcelTools.readHubs(excelFile);
        Customer[] customers = ExcelTools.readCustomers(excelFile);

        // Test Vars
//        Random ran = new Random();
        int nbProduits = 3;
//        int qProduits = 100;

        // Sets
//        Producer[] producers = {new Producer(0, "Fiction", 45.14429, 5.20811),
//                new Producer(1, "Ferme1", 45.14429, 5.20811),
//                new Producer(2, "Ferme2", 45.71531, 5.67431),
//                new Producer(3, "Ferme3", 45.52911, 5.73944)};
//
//        for (Producer producer : producers) {
//            if (producer.getName().equals("Fiction")) {
//                continue;
//            }
//            producer.setSupply("Produits laitiers vache", ran.nextInt(qProduits));
//            producer.setSupply("Produits laitiers chèvre", ran.nextInt(qProduits));
//            producer.setSupply("Fruits", ran.nextInt(qProduits));
//        }

//        Hub[] hubs = {new Hub(1, "Voiron", 17000, 45.35276, 5.56985),
//                new Hub(2, "MIN de Grenoble", 15500, 45.17232, 5.71741)};

        double[][] openCost = new double[hubs.length][1];
        for (int i = 0; i < hubs.length; i++) {
            openCost[i][0] = (double) hubs[i].getOpCost();
        }

//        Customer[] customers = {new Customer(0, "Fiction", "Supermarché", 45.17823, 5.74396),
//                new Customer(1, "Client 1", "Supermarché", 45.17823, 5.74396),
//                new Customer(2, "Client 2", "Supermarché", 45.4327231, 6.0192055),
//                new Customer(3, "Client 3", "Supermarché", 45.1901677, 5.6940435),
//                new Customer(4, "Client 4", "Supermarché", 45.5967377, 5.0944433),
//                new Customer(5, "Client 5", "Supermarché", 45.6732628, 5.4846254)};
//
//        for (Customer customer : customers) {
//            if (customer.getName().equals("Fiction")) {
//                continue;
//            }
//            customer.setDemand("Produits laitiers vache", ran.nextInt(qProduits));
//            customer.setDemand("Produits laitiers chèvre", ran.nextInt(qProduits));
//            customer.setDemand("Fruits", ran.nextInt(qProduits));
//        }

        System.out.println("Calculating initial O/D...");
        // Offer / Demand
        double[][] offer = new double[producers.length][nbProduits];
        double[][] demand = new double[customers.length][nbProduits];
        calculateOffer(producers, offer);
        calculateDemand(customers, demand);

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


        System.out.println("Adding fictive O/D...");
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
        double[][][] cPH = new double[producers.length][hubs.length][nbProduits];
        // Hub => Client
        double[][][] cHC = new double[hubs.length][customers.length][nbProduits];
        // Prod => Client
        double[][][] cPC = new double[producers.length][customers.length][nbProduits];
        // Hub => Hub
        double[][][] cHH = new double[hubs.length][hubs.length][nbProduits];

        // Inclusion de la O/D fictive
        calculateOffer(producers, offer);
        calculateDemand(customers, demand);

        System.out.println("Calculating shipping costs, this might take a while...");
        // Calcul des couts de transport en fonction de la distance (km)

        calculateShippingCosts(producers, hubs, customers,nbProduits, cPH, cHC, cPC, cHH);

//        cPH = new double[][][]{{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}}
//                , {{56.0, 56.0, 56.0}, {67.0, 67.0, 67.0}}
//                , {{49.0, 49.0, 49.0}, {95.0, 95.0, 95.0}}
//                , {{30.0, 30.0, 30.0}, {57.0, 57.0, 57.0}}};
//
//        cHC = new double[][][]{{{0.0, 0.0, 0.0}, {13.5, 13.5, 13.5}, {28.5, 28.5, 28.5}, {11.5, 11.5, 11.5}, {30.5, 30.5, 30.5}, {21.5, 21.5, 21.5}},
//                {{0.0, 0.0, 0.0}, {1.0, 1.0, 1.0}, {23.0, 23.0, 23.0}, {2.0, 2.0, 2.0}, {43.0, 43.0, 43.0}, {42.5, 42.5, 42.5}}};
//
//
//        cPC = new double[][][]{{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}},
//                {{0.0, 0.0, 0.0}, {66.0, 66.0, 66.0}, {109.0, 109.0, 109.0}, {62.0, 62.0, 62.0}, {68.0, 68.0, 68.0}, {82.0, 82.0, 82.0}},
//                {{0.0, 0.0, 0.0}, {88.0, 88.0, 88.0}, {55.0, 55.0, 55.0}, {98.0, 98.0, 98.0}, {61.0, 61.0, 61.0}, {22.0, 22.0, 22.0}},
//                {{0.0, 0.0, 0.0}, {52.0, 52.0, 52.0}, {46.0, 46.0, 46.0}, {52.0, 52.0, 52.0}, {61.0, 61.0, 61.0}, {29.0, 29.0, 29.0}}};
//
//        cHH = new double[][][]{{{0.0, 0.0, 0.0}, {14.0, 14.0, 14.0}}, {{13.5, 13.5, 13.5}, {0.0, 0.0, 0.0}}};
        System.out.println("Costs calculated");



         Create the optimization problem object
        OptimizationProblem op = new OptimizationProblem();


        System.out.println("Adding decision variables...");
         Add the decision variables to the problem
        op.addDecisionVariable("isOpen", true, new int[]{hubs.length, 1}, 0, 1);  // name, isInteger, size , minValue, maxValue
        // Nombre de produits à transferer
        op.addDecisionVariable("yPC", true, new int[]{producers.length, customers.length, nbProduits}, 0, M);
        op.addDecisionVariable("yPH", true, new int[]{producers.length, hubs.length, nbProduits}, 0, M);
        op.addDecisionVariable("yHC", true, new int[]{hubs.length, customers.length, nbProduits}, 0, M);
        op.addDecisionVariable("yHH", true, new int[]{hubs.length, hubs.length, nbProduits}, 0, M);

        System.out.println("Preparing input parameters...");
         Set value for the input parameters
        op.setInputParameter("openCost", new DoubleMatrixND(openCost));
        op.setInputParameter("offer", new DoubleMatrixND(offer));
        op.setInputParameter("demand", new DoubleMatrixND(demand));
        op.setInputParameter("M", M);

        // Cout de transfert
        op.setInputParameter("cPH", new DoubleMatrixND(cPH));
        op.setInputParameter("cHC", new DoubleMatrixND(cHC));
        op.setInputParameter("cPC", new DoubleMatrixND(cPC));
        op.setInputParameter("cHH", new DoubleMatrixND(cHH));



        System.out.println("Generating constraints...");

         Add the constraints
        // produits sortants par producteur == Offre du producteur
        op.addConstraint("sum(yPC,2) + sum(yPH,2) == offer");

        // produits sortants par hub == Somme des produits entrants par hub
        op.addConstraint("sum(yPH,1) - sum(yHC,2) == 0 ");

        // produits entrants par client == Demande du client
        op.addConstraint("sum(yPC,1) + sum(yHC,1) == demand");

        // Contrainte ouvertue Hub, s'il existe un flux entre un producteur et un hub , le hub est alors considéré ouvert
        op.addConstraint("sum(sum(yPH,3),1) <= M * sum(isOpen,1)");


        System.out.println("Setting objective functions...");
         Sets the objective function
        op.setObjectiveFunction("minimize", "sum(isOpen .* openCost) + sum(cPH .* yPH) + sum(cHH .* yHH) + sum(cHC .* yHC) + sum(cPC .* yPC)");


         Call the solver to solve the problem
        System.out.println("Solving...");
        op.solve("glpk", "solverLibraryName", "res/glpk/glpk");
        if (!op.solutionIsOptimal()) {
            throw new RuntimeException("An optimal solution was not found");
        }else{
            System.out.println("Optimal solution found!");
        }


         Print the solution
//        System.out.println(op.getPrimalSolution("isOpen").toString());
        System.out.println("\nOptimal cost: "+op.getOptimalCost()+"\n");
//        System.out.println(op.getPrimalSolution("yPH"));
//        System.out.println(op.getPrimalSolution("yHH"));
//        System.out.println(op.getPrimalSolution("yPC"));
//        System.out.println(op.getPrimalSolution("yHC"));

        String[] results = op.getPrimalSolution("isOpen").toString().split(";;");
        int idx = 0;
        for (String res: results){
            double open = Double.parseDouble(res);
            if (open == 1.0) {
                System.out.println(hubs[idx].getName()+ " hub is OPEN\n");
            }
            idx++;
        }
    }

    private static void calculateShippingCosts(Producer[] producers, Hub[] hubs, Customer[] customers, int nbProduits, double[][][] cPH, double[][][] cHC, double[][][] cPC, double[][][] cHH) throws Exception {

        double costPtoX = 1.0;
        double costHtoX = 0.5;
        int coefP; // Coef prod fictif
        int coefC; // Coef client fictif
        double cost;
        for (int i = 0; i < producers.length; i++) {
            if (producers[i].getName().equals("Fiction")) {
                coefP = 0;
            } else {
                coefP = 1;
            }
            for (int j = 0; j < hubs.length; j++) {
                cost = (producers[i].getDistanceTo(hubs[j]) / 1000) * costPtoX * coefP;
                for (int l = 0; l<nbProduits; l++){
                    cPH[i][j][l] = cost;
                }
            }
            for (int k = 0; k < customers.length; k++) {
                if (customers[i].getName().equals("Fiction")) {
                    coefC = 0;
                } else {
                    coefC = 1;
                }
                cost = (producers[i].getDistanceTo(customers[k]) / 1000) * costPtoX * coefC * coefP;
                for (int l = 0; l<nbProduits; l++){
                    cPC[i][k][l] = cost;
                }

            }
        }

        for (int j = 0; j < hubs.length; j++) {
            for (int h = 0; h < hubs.length; h++) {
                cost = (hubs[j].getDistanceTo(hubs[h]) / 1000) * costHtoX;
                for (int l = 0; l<nbProduits; l++){
                    cHH[j][h][l] =  cost;
                }
            }
            for (int k = 0; k < customers.length; k++) {
                if (customers[k].getName().equals("Fiction")) {
                    coefC = 0;
                } else {
                    coefC = 1;
                }
                cost =                 (hubs[j].getDistanceTo(customers[k]) / 1000) * costHtoX * coefC;

                for (int l = 0; l<nbProduits; l++){
                    cHC[j][k][l] =  cost;
                }
            }
        }
    }

    private static void calculateDemand(Customer[] customers, double[][] demand) {
        for (int i = 0; i < customers.length; i++) {
            Map custDemand = customers[i].getDemand();
            int j = 0;
            for (Object value : custDemand.values()) {
                demand[i][j] = (double) (int) value;
                j++;
            }
        }
    }

    private static void calculateOffer(Producer[] producers, double[][] offer) {
        for (int i = 0; i < producers.length; i++) {
            Map prodSupply = producers[i].getSupply();
            int j = 0;
            for (Object value : prodSupply.values()) {
                offer[i][j] = (double) (int) value;
                j++;
            }
        }
    }
}