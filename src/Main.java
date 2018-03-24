
//import Tools.ExcelTools;

import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import models.*;
//import org.junit.frame;
import sun.misc.Unsafe;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.*;

import static java.lang.Integer.max;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.*;

public class Main {
    private static ArrayList<Hub> chosenHubs = new ArrayList<>();
    private static OptimizationProblem op;
    private static Producer[] producers;
    private static Customer[] customers;
    private static Hub[] hubs;
    private static boolean useAPI;
    private static int bigM;

    public static void main(String[] args) throws Exception {

        /* Disable Warnings */
        disableWarning();

        /* Get Excel file */
//        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx"); // Launching with IDE
//        File excelFile = new File("res/Projet_DistAgri_Inst_Moyenne.xlsx"); // Launching with IDE
//        File excelFile = new File("res/Projet_DistAgri_Inst_Grande.xlsx"); // Launching with IDE
        //File excelFile = new File(args[0]);

        System.out.println("Adding locations...");
        /* Initialise variables */
//        producers = ExcelTools.readProducers(excelFile);
//        hubs = ExcelTools.readHubs(excelFile);
//        customers = ExcelTools.readCustomers(excelFile);

        // frame Vars
        Random ran = new Random();
//        int nbProduits = max(producers[1].getSupply().size(), customers[1].getDemand().size());
        int nbProduits = 3;
        int qProduits = 200;
        int nbPeriodes = 10;

//         Sets
        producers = new Producer[]{new Producer(0, "Fiction", 45.14429, 5.20811),
                new Producer(1, "Ferme1", 45.14429, 5.20811),
                new Producer(2, "Ferme2", 45.71531, 5.67431),
                new Producer(3, "Ferme3", 45.52911, 5.73944)};

        for (Producer producer : producers) {
            if (producer.getName().equals("Fiction")) {
                continue;
            }
            for (int i = 0; i < nbPeriodes; i++) {
                producer.setSupply(i, "Produits laitiers vache", ran.nextInt(qProduits));
                producer.setSupply(i, "Produits laitiers chèvre", ran.nextInt(qProduits));
                producer.setSupply(i, "Fruits", ran.nextInt(qProduits));
            }
        }

        hubs = new Hub[]{new Hub(1, "Voiron", 0, 45.35276, 5.56985),
                new Hub(2, "MIN de Grenoble", 0, 45.17232, 5.71741)};

        double[][] openCost = new double[hubs.length][1];
        for (int i = 0; i < hubs.length; i++) {
            openCost[i][0] = (double) hubs[i].getOpCost();
        }

        customers = new Customer[]{new Customer(0, "Fiction", "Supermarché", 45.17823, 5.74396),
                new Customer(1, "Client 1", "Supermarché", 45.17823, 5.74396),
                new Customer(2, "Client 2", "Supermarché", 45.4327231, 6.0192055),
                new Customer(3, "Client 3", "Supermarché", 45.1901677, 5.6940435),
                new Customer(4, "Client 4", "Supermarché", 45.5967377, 5.0944433),
                new Customer(5, "Client 5", "Supermarché", 45.6732628, 5.4846254)};

        for (Customer customer : customers) {
            if (customer.getName().equals("Fiction")) {
                continue;
            }
            for (int i = 0; i < nbPeriodes; i++) {
                customer.setDemand(i, "Produits laitiers vache", ran.nextInt(qProduits));
                customer.setDemand(i, "Produits laitiers chèvre", ran.nextInt(qProduits));
                customer.setDemand(i, "Fruits", ran.nextInt(qProduits));
            }
        }

        System.out.println("Calculating initial O/D...");
        // Offer / Demand
        double[][][] offer = new double[producers.length][nbPeriodes][nbProduits];
        double[][][] demand = new double[customers.length][nbPeriodes][nbProduits];
        calculateOffer(offer, 1);
        calculateDemand(demand, 1);

        int[][] prodOffer = new int[nbPeriodes][nbProduits];
        for (double[][] subOffer : offer) {
            for (int i = 0; i < nbPeriodes; i++) {
                for (int currentProd = 0; currentProd < nbProduits; currentProd++) {
                    prodOffer[i][currentProd] += subOffer[i][currentProd];
                }
            }

        }

        int[][] prodDemand = new int[nbPeriodes][nbProduits];
        for (double[][] subDemand : demand) {
            for (int i = 0; i < nbPeriodes; i++) {
                for (int currentProd = 0; currentProd < nbProduits; currentProd++) {
                    prodDemand[i][currentProd] += subDemand[i][currentProd];
                }
            }
        }


        System.out.println("Adding fictive O/D...");
        // Fictive offer/demand
        for (int i = 0; i < nbPeriodes; i++) {
            for (int j = 0; j < nbProduits; j++) {
                int offset = prodOffer[i][j] - prodDemand[i][j];
                if (prodDemand[i][j] > prodOffer[i][j]) {
                    producers[0].setSupply(i, "Produit fictif " + j, -offset);
                    customers[0].setDemand(i, "Produit fictif " + j, 0);
                } else {
                    customers[0].setDemand(i, "Produit fictif " + j, offset);
                    producers[0].setSupply(i, "Produit fictif " + j, 0);
                }
            }
        }


        // Shipping cost using distances
        // Prod => Hub
        double[][][][] cPH = new double[producers.length][hubs.length][nbPeriodes][nbProduits];
        // Hub => Client
        double[][][][] cHC = new double[hubs.length][customers.length][nbPeriodes][nbProduits];
        // Prod => Client
        double[][][][] cPC = new double[producers.length][customers.length][nbPeriodes][nbProduits];
        // Hub => Hub
        double[][][][] cHH = new double[hubs.length][hubs.length][nbPeriodes][nbProduits];

        // Inclusion de la O/D fictive
        calculateOffer(offer, 0);
        calculateDemand(demand, 0);

        System.out.println("Calculating shipping costs, this might take a while...");
        // Calcul des couts de transport en fonction de la distance (km)

//        calculateShippingCosts(producers, hubs, customers, nbProduits, cPH, cHC, cPC, cHH, excelFile);
        calculateShippingCosts(producers, hubs, customers, nbPeriodes, nbProduits, cPH, cHC, cPC, cHH, null);

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

        double[][] bigMatrix = new double[hubs.length][nbPeriodes];
        // Fill each row with 1.0
        for (double[] row: bigMatrix)
            Arrays.fill(row,bigM);


        /* Create the optimization problem object */
        op = new OptimizationProblem();


        System.out.println("Adding decision variables...");
        /* Add the decision variables to the problem */
        op.addDecisionVariable("isOpen", true, new int[]{hubs.length,1}, 0, 1);  // name, isInteger, size , minValue, maxValue
        // Nombre de produits à transferer
        op.addDecisionVariable("yPC", true, new int[]{producers.length, customers.length, nbPeriodes, nbProduits}, 0, bigM);
        op.addDecisionVariable("yPH", true, new int[]{producers.length, hubs.length, nbPeriodes, nbProduits}, 0, bigM);
        op.addDecisionVariable("yHC", true, new int[]{hubs.length, customers.length, nbPeriodes, nbProduits}, 0, bigM);
        op.addDecisionVariable("yHH", true, new int[]{hubs.length, hubs.length, nbPeriodes, nbProduits}, 0, bigM);

        System.out.println("Preparing input parameters...");
        /* Set value for the input parameters */
        op.setInputParameter("openCost", new DoubleMatrixND(openCost));
        op.setInputParameter("offer", new DoubleMatrixND(offer));
        op.setInputParameter("demand", new DoubleMatrixND(demand));
        op.setInputParameter("M", bigM);
        op.setInputParameter("bigMatrix", bigMatrix);

        // Cout de transfert
        op.setInputParameter("cPH", new DoubleMatrixND(cPH));
        op.setInputParameter("cHC", new DoubleMatrixND(cHC));
        op.setInputParameter("cPC", new DoubleMatrixND(cPC));
        op.setInputParameter("cHH", new DoubleMatrixND(cHH));


        System.out.println("Generating constraints...");

        /* Add the constraints */
        // produits sortants par producteur == Offre du producteur
        op.addConstraint("sum(yPC,2) + sum(yPH,2) == offer");

        // produits sortants par hub == Somme des produits entrants par hub
        op.addConstraint("sum(yPH,1) - sum(yHC,2) == 0 ");

        // produits entrants par client == Demande du client
        op.addConstraint("sum(yPC,1) + sum(yHC,1) == demand");

        // Contrainte ouverture Hub, s'il existe un flux entre un producteur et un hub ,peu importe la periode, le hub est alors considéré ouvert
        op.addConstraint("sum(sum(sum(yPH,4),3),1) <= M * sum(isOpen,1)");

        // Contrainte ouverture Hub, s'il existe un flux entre un client et un hub,peu importe la periode, le hub est alors considéré ouvert
        op.addConstraint("sum(sum(sum(yHC,4),3),2) <= M * sum(isOpen,1)");


        System.out.println("Setting objective functions...");
        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum(isOpen .* openCost) + sum(cPH .* yPH) + sum(cHH .* yHH) + sum(cHC .* yHC) + sum(cPC .* yPC)");


        /* Call the solver to solve the problem */
        System.out.println("Solving...");

        op.solve("glpk", "solverLibraryName", "glpk");
        if (!op.solutionIsOptimal()) {
            throw new RuntimeException("An optimal solution was not found");
        } else {
            System.out.println("Optimal solution found!");
        }

        /* Print the solution */
        System.out.println("\nOptimal cost: " + op.getOptimalCost() + "\n");


//        System.out.println(op.getPrimalSolution("yPH"));
//        System.out.println(op.getPrimalSolution("yPC"));
//        System.out.println(op.getPrimalSolution("yHC"));

//        System.out.println(op.getPrimalSolution("isOpen"));


        displayResults(producers, hubs, customers, op);
    }

    public static void displayResults(Producer[] producers, Hub[] hubs, Customer[] customers, OptimizationProblem op) {
        String[] results = op.getPrimalSolution("isOpen").toString().split(";;");
        int idx = 0;
        for (String res : results) {
            double open = Double.parseDouble(res);
            if (open == 1.0) {
                chosenHubs.add(hubs[idx]);
                //System.out.println(Arrays.deepToString(sumMatrix("yHC")));
                System.out.println(hubs[idx].getName() + " hub is OPEN\n");
            }
            idx++;
        }
        if (true) {

            // get the screen size as a java dimension
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            // set the jframe to take half of the screen
            int height = screenSize.height / 2;
            int width = screenSize.width / 2;


            JFrame frame = new JFrame("Google Maps");
            // set the jframe height and width
            frame.setPreferredSize(new Dimension(600, 600));
            //Ajout des producteurs
            String mapString = "";
            for (int i = 1; i < producers.length; i++) {
//                mapString += "&markers=icon:/F.png%7C" + Double.toString(producers[i].getLongitude()) + "," + Double.toString(producers[i].getLatitude());
                mapString += "&markers=icon:/F.png%7C" + Double.toString(producers[i].getLatitude()) + "," + Double.toString(producers[i].getLongitude());
            }
            for (int i = 1; i < customers.length; i++) {
//                mapString += "&markers=icon:http://pierret.pro/C.png%7C" + Double.toString(customers[i].getLongitude()) + "," + Double.toString(customers[i].getLatitude());
                mapString += "&markers=icon:http://pierret.pro/C.png%7C" + Double.toString(customers[i].getLatitude()) + "," + Double.toString(customers[i].getLongitude());
            }
            for (int i = 0; i < chosenHubs.size(); i++) {
//                mapString += "&markers=icon:http://pierret.pro/H.png%7C" + Double.toString(hubs[chosenHubs.get(i)].getLongitude()) + "," + Double.toString(hubs[chosenHubs.get(i)].getLatitude());
//                System.out.println(chosenHubs.get(i).getName());
                mapString += "&markers=icon:http://pierret.pro/H.png%7C" + Double.toString(chosenHubs.get(i).getLatitude()) + "," + Double.toString(chosenHubs.get(i).getLongitude());
            }

            try {
//                String latitude = "45.1934574";
//                String longitude = "5.7682659";
                String imageUrl = "https://maps.googleapis.com/maps/api/staticmap?size=4096x4096&scale=2&maptype=roadmap"
                        + mapString
                        + linkTwoPoints(sumMatrix("yPC"), "yPC", "0000ff")
                        + linkTwoPoints(sumMatrix("yHC"), "yHC", "00ff00")
                        + linkTwoPoints(sumMatrix("yPH"), "yPH", "ff0000");
//                System.out.println(imageUrl);
                String destinationFile = "image.jpg";
                // read the map image from Google
                // then save it to a local file: image.jpg
                //
                URL url = new URL(imageUrl);
                InputStream is = url.openStream();
                OutputStream os = new FileOutputStream(destinationFile);
                byte[] b = new byte[2048];
                int length;
                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }
                is.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            // create a GUI component that loads the image: image.jpg
            //
            ImageIcon imageIcon = new ImageIcon((new ImageIcon("image.jpg"))
                    .getImage().getScaledInstance(600, 600,
                            java.awt.Image.SCALE_SMOOTH));
            frame.add(new JLabel(imageIcon));
            // show the GUI window
            frame.setVisible(true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }
    }

    private static void calculateShippingCosts(Producer[] producers, Hub[] hubs, Customer[] customers, int nbPeriodes, int nbProduits, double[][][][] cPH, double[][][][] cHC, double[][][][] cPC, double[][][][] cHH, File excelFile) throws Exception {

//        double costPtoC = ExcelTools.readCost(excelFile, "PtoC");
//        double costHtoC = ExcelTools.readCost(excelFile, "HtoC");
//        double costPtoH = ExcelTools.readCost(excelFile, "PtoH");
//        double costHtoH = ExcelTools.readCost(excelFile, "HtoH");
        double costPtoC = 1;
        double costHtoC = 0.5;
        double costPtoH = 1;
        double costHtoH = 0.5;
        int coefP; // Coef prod fictif
        int coefC; // Coef client fictif
        double cost;

        //Définir si on utilise l'API
        int nbPaths = customers.length * hubs.length + producers.length * hubs.length + producers.length * customers.length + hubs.length * hubs.length;

        if (nbPaths > 7500) {
            useAPI = false;
        }
        //else useAPI = true;
        else useAPI = false;
        for (int i = 0; i < producers.length; i++) {
            if (producers[i].getName().equals("Fiction")) {
                coefP = 0;
            } else {
                coefP = 1;
            }
            for (int j = 0; j < hubs.length; j++) {
                cost = (producers[i].getDistanceTo(hubs[j], useAPI) / 1000) * costPtoH * coefP;
                for (int k = 0; k < nbPeriodes; k++) {
                    for (int l = 0; l < nbProduits; l++) {
                        cPH[i][j][k][l] = cost;
                    }
                }
            }
            for (int j = 0; j < customers.length; j++) {
                if (customers[i].getName().equals("Fiction")) {
                    coefC = 0;
                } else {
                    coefC = 1;
                }
                cost = (producers[i].getDistanceTo(customers[j], useAPI) / 1000) * costPtoC * coefC * coefP;
                for (int l = 0; l < nbProduits; l++) {
                    for (int k = 0; k < nbPeriodes; k++) {
                        cPC[i][j][k][l] = cost;
                    }
                }

            }
        }

        for (int i = 0; i < hubs.length; i++) {
            for (int j = 0; j < hubs.length; j++) {
                cost = (hubs[i].getDistanceTo(hubs[j], useAPI) / 1000) * costHtoH;
                for (int k = 0; k < nbPeriodes; k++) {
                    for (int l = 0; l < nbProduits; l++) {
                        cHH[i][j][k][l] = cost;
                    }
                }
            }
            for (int j = 0; j < customers.length; j++) {
                if (customers[j].getName().equals("Fiction")) {
                    coefC = 0;
                } else {
                    coefC = 1;
                }
                cost = (hubs[i].getDistanceTo(customers[j], useAPI) / 1000) * costHtoC * coefC;
                for (int k = 0; k < nbPeriodes; k++) {
                    for (int l = 0; l < nbProduits; l++) {
                        cHC[i][j][k][l] = cost;
                    }
                }
            }
        }
    }

    private static void calculateDemand(double[][][] demand, int start) {
        for (int i = start; i < customers.length; i++) {
            Map<Integer, Map<String, Integer>> prodDemand = customers[i].getDemand();
            for (int j = 0; j < prodDemand.size(); j++) {
                Map<String, Integer> period = prodDemand.get(j);
                int k = 0;
                for (Map.Entry<String, Integer> pair : period.entrySet()) {
                    double value = pair.getValue();
                    if (value > bigM) {
                        bigM = 100 * (int) value;
                    }
                    demand[i][j][k] = (double) (int) value;
                    k++;
                }
            }
        }
    }

    private static void calculateOffer(double[][][] offer, int start) {
        for (int i = start; i < producers.length; i++) {
            Map<Integer, Map<String, Integer>> prodSupply = producers[i].getSupply();
            for (int j = 0; j < prodSupply.size(); j++) {
                Map<String, Integer> period = prodSupply.get(j);
                int k = 0;
                for (Map.Entry<String, Integer> pair : period.entrySet()) {
                    double value = pair.getValue();
                    if (value > bigM) {
                        bigM = 100 * (int) value;
                    }
                    offer[i][j][k] = (double) (int) value;
                    k++;
                }
            }
        }
    }

    private static String linkTwoPoints(int[][] matrix, String matrixType, String hexaColor) {
        String result = "";
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrixType.equals("yPC") && !producers[i].getName().equals("Fiction") && !customers[j].getName().equals("Fiction") && matrix[i][j] != 0) {
//                    result += "&path=" + Double.toString(producers[i].getLongitude()) + "," + Double.toString(producers[i].getLatitude()) + "|" + Double.toString(customers[j].getLongitude()) + "," + Double.toString(customers[j].getLatitude());
                    result += "&path=" + Double.toString(producers[i].getLatitude()) + "," + Double.toString(producers[i].getLongitude()) + "|" + Double.toString(customers[j].getLatitude()) + "," + Double.toString(customers[j].getLongitude());
                }
                if (matrixType.equals("yHC") && !customers[j].getName().equals("Fiction") && matrix[i][j] != 0 && chosenHubs.contains(hubs[i])) {
//                    result += "&path=" + Double.toString(hubs[i].getLongitude()) + "," + Double.toString(hubs[i].getLatitude()) + "|" + Double.toString(customers[j].getLongitude()) + "," + Double.toString(customers[j].getLatitude());
                    result += "&path=" + Double.toString(hubs[i].getLatitude()) + "," + Double.toString(hubs[i].getLongitude()) + "|" + Double.toString(customers[j].getLatitude()) + "," + Double.toString(customers[j].getLongitude());
//                    System.out.println("ADDED :"+hubs[i].getName());
                }
                if (matrixType.equals("yPH") && !producers[i].getName().equals("Fiction") && matrix[i][j] != 0 && chosenHubs.contains(hubs[j])) {
//                    result += "&path=" + Double.toString(producers[i].getLongitude()) + "," + Double.toString(producers[i].getLatitude()) + "|" + Double.toString(hubs[j].getLongitude()) + "," + Double.toString(hubs[j].getLatitude());
                    result += "&path=" + Double.toString(producers[i].getLatitude()) + "," + Double.toString(producers[i].getLongitude()) + "|" + Double.toString(hubs[j].getLatitude()) + "," + Double.toString(hubs[j].getLongitude());
//                    System.out.println("ADDED :"+hubs[i].getName());
                }
            }
        }
        return result;
    }

    private static int[][] sumMatrix(String matrixName) {
        int[][] newMatrix = new int[op.getPrimalSolution(matrixName).getSize(0)][op.getPrimalSolution(matrixName).getSize(1)];
        for (int i = 0; i < op.getPrimalSolution(matrixName).getSize(0); i++) {
            for (int j = 0; j < op.getPrimalSolution(matrixName).getSize(1); j++) {
                int sum = 0;
                for (int k = 0; k < op.getPrimalSolution(matrixName).getSize(2); k++) {
                    int[] subIndexes = {i, j, k};
                    sum += op.getPrimalSolution(matrixName).get(subIndexes);
                }
                newMatrix[i][j] = sum;
            }

        }
        return newMatrix;
    }

    private static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

}