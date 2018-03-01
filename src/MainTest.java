import Tools.ExcelTools;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import models.Customer;
import models.Hub;
import models.Location;
import models.Producer;
import sun.misc.Unsafe;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Integer.max;

public class MainTest {
    static List<Integer> chosenHubs = new ArrayList<>();
    static OptimizationProblem op;
    static Producer[] producers = {new Producer(0, "Fiction", 45.14429, 5.20811),
            new Producer(1, "Ferme1", 45.14429, 5.20811),
            new Producer(2, "Ferme2", 45.71531, 5.67431),
            new Producer(3, "Ferme3", 45.52911, 5.73944)};
    static Customer[] customers = {new Customer(0, "Fiction", "Supermarché", 45.17823, 5.74396),
            new Customer(1, "Client 1", "Supermarché", 45.17823, 5.74396),
            new Customer(2, "Client 2", "Supermarché", 45.4327231, 6.0192055),
            new Customer(3, "Client 3", "Supermarché", 45.1901677, 5.6940435),
            new Customer(4, "Client 4", "Supermarché", 45.5967377, 5.0944433),
            new Customer(5, "Client 5", "Supermarché", 45.6732628, 5.4846254)};
    static Hub[] hubs = {new Hub(1, "Voiron", 17000, 45.35276, 5.56985),
            new Hub(2, "MIN de Grenoble", 15500, 45.17232, 5.71741)};

    public static void main(String[] args) throws Exception {
        /* Disable Warnings */
        disableWarning();

        /* Get Excel file */
        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx");

        System.out.println("Adding locations...");
        /* Initialise variables */
//        Producer[] producers = ExcelTools.readProducers(excelFile);
//        Hub[] hubs = ExcelTools.readHubs(excelFile);
//        Customer[] customers = ExcelTools.readCustomers(excelFile);

        // Test Vars
        Random ran = new Random();
        int qProduits = 9000;

        // Sets


        for (Producer producer : producers) {
            if (producer.getName().equals("Fiction")) {
                continue;
            }
            producer.setSupply("Produits laitiers vache", ran.nextInt(qProduits));
            producer.setSupply("Produits laitiers chèvre", ran.nextInt(qProduits));
            producer.setSupply("Fruits", ran.nextInt(qProduits));
        }


        double[][] openCost = new double[hubs.length][1];
        for (int i = 0; i < hubs.length; i++) {
            openCost[i][0] = (double) hubs[i].getOpCost();
        }


        for (Customer customer : customers) {
            if (customer.getName().equals("Fiction")) {
                continue;
            }
            customer.setDemand("Produits laitiers vache", ran.nextInt(qProduits));
            customer.setDemand("Produits laitiers chèvre", ran.nextInt(qProduits));
            customer.setDemand("Fruits", ran.nextInt(qProduits));
        }

        System.out.println("Calculating initial O/D...");
        // Offer / Demand
        int nbProduits = max(producers[1].getSupply().size(), customers[1].getDemand().size());
        double[][] offer = new double[producers.length][nbProduits];
        double[][] demand = new double[customers.length][nbProduits];
        calculateOffer(producers, offer);
        calculateDemand(customers, demand);

        int[] prodOffer = new int[nbProduits];
        for (double[] subOffer : offer) {
            for (int currentProd = 0; currentProd < nbProduits; currentProd++) {
                prodOffer[currentProd] += subOffer[currentProd];

            }
        }

        int[] prodDemand = new int[nbProduits];
        for (double[] subDemand : demand) {
            for (int currentProd = 0; currentProd < nbProduits; currentProd++) {
                prodDemand[currentProd] += subDemand[currentProd];
            }
        }

        // Big M
        int totalOffer = IntStream.of(prodOffer).sum();
        int totalDemand = IntStream.of(prodDemand).sum();
        int M = max(totalOffer, totalDemand);


        System.out.println("Adding fictive O/D...");
        // Fictive offer/demand
        for (int i = 0; i < nbProduits; i++) {
            int offset = prodOffer[i] - prodDemand[i];
            if (prodDemand[i] > prodOffer[i]) {
                producers[0].setSupply("Produit fictif " + i, -offset);
                customers[0].setDemand("Produit fictif " + i, 0);
            } else {
                customers[0].setDemand("Produit fictif " + i, offset);
                producers[0].setSupply("Produit fictif " + i, 0);
            }
        }


        // Shipping cost using distances
        // Prod => Hub
//        double[][][] cPH = new double[producers.length][hubs.length][nbProduits];
//        // Hub => Client
//        double[][][] cHC = new double[hubs.length][customers.length][nbProduits];
//        // Prod => Client
//        double[][][] cPC = new double[producers.length][customers.length][nbProduits];
//        // Hub => Hub
//        double[][][] cHH = new double[hubs.length][hubs.length][nbProduits];

        // Inclusion de la O/D fictive
        calculateOffer(producers, offer);
        calculateDemand(customers, demand);

        System.out.println("Calculating shipping costs, this might take a while...");
        // Calcul des couts de transport en fonction de la distance (km)

//        calculateShippingCosts(producers, hubs, customers, nbProduits, cPH, cHC, cPC, cHH, excelFile);

        double[][][] cPH = new double[][][]{{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}}
                , {{56.0, 56.0, 56.0}, {67.0, 67.0, 67.0}}
                , {{49.0, 49.0, 49.0}, {95.0, 95.0, 95.0}}
                , {{30.0, 30.0, 30.0}, {57.0, 57.0, 57.0}}};

        double[][][] cHC = new double[][][]{{{0.0, 0.0, 0.0}, {13.5, 13.5, 13.5}, {28.5, 28.5, 28.5}, {11.5, 11.5, 11.5}, {30.5, 30.5, 30.5}, {21.5, 21.5, 21.5}},
                {{0.0, 0.0, 0.0}, {1.0, 1.0, 1.0}, {23.0, 23.0, 23.0}, {2.0, 2.0, 2.0}, {43.0, 43.0, 43.0}, {42.5, 42.5, 42.5}}};


        double[][][] cPC = new double[][][]{{{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}},
                {{0.0, 0.0, 0.0}, {66.0, 66.0, 66.0}, {109.0, 109.0, 109.0}, {62.0, 62.0, 62.0}, {68.0, 68.0, 68.0}, {82.0, 82.0, 82.0}},
                {{0.0, 0.0, 0.0}, {88.0, 88.0, 88.0}, {55.0, 55.0, 55.0}, {98.0, 98.0, 98.0}, {61.0, 61.0, 61.0}, {22.0, 22.0, 22.0}},
                {{0.0, 0.0, 0.0}, {52.0, 52.0, 52.0}, {46.0, 46.0, 46.0}, {52.0, 52.0, 52.0}, {61.0, 61.0, 61.0}, {29.0, 29.0, 29.0}}};
//
        double[][][] cHH = new double[][][]{{{0.0, 0.0, 0.0}, {14.0, 14.0, 14.0}}, {{13.5, 13.5, 13.5}, {0.0, 0.0, 0.0}}};
        System.out.println("Costs calculated");



        /* Create the optimization problem object */
        op = new OptimizationProblem();


        System.out.println("Adding decision variables...");
        /* Add the decision variables to the problem */
        op.addDecisionVariable("isOpen", true, new int[]{hubs.length, 1}, 0, 1);  // name, isInteger, size , minValue, maxValue
        // Nombre de produits à transferer
        op.addDecisionVariable("yPC", true, new int[]{producers.length, customers.length, nbProduits}, 0, M);
        op.addDecisionVariable("yPH", true, new int[]{producers.length, hubs.length, nbProduits}, 0, M);
        op.addDecisionVariable("yHC", true, new int[]{hubs.length, customers.length, nbProduits}, 0, M);
        op.addDecisionVariable("yHH", true, new int[]{hubs.length, hubs.length, nbProduits}, 0, M);

        System.out.println("Preparing input parameters...");
        /* Set value for the input parameters */
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

        /* Add the constraints */
        // produits sortants par producteur == Offre du producteur
        op.addConstraint("sum(yPC,2) + sum(yPH,2) == offer");

        // produits sortants par hub == Somme des produits entrants par hub
        op.addConstraint("sum(yPH,1) - sum(yHC,2) == 0 ");

        // produits entrants par client == Demande du client
        op.addConstraint("sum(yPC,1) + sum(yHC,1) == demand");

        // Contrainte ouvertue Hub, s'il existe un flux entre un producteur et un hub , le hub est alors considéré ouvert
        op.addConstraint("sum(sum(yPH,3),1) <= M * sum(isOpen,1)");


        System.out.println("Setting objective functions...");
        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum(isOpen .* openCost) + sum(cPH .* yPH) + sum(cHH .* yHH) + sum(cHC .* yHC) + sum(cPC .* yPC)");


        /* Call the solver to solve the problem */
        System.out.println("Solving...");

        op.solve("glpk", "solverLibraryName", "res/glpk/glpk");
        if (!op.solutionIsOptimal()) {
            throw new RuntimeException("An optimal solution was not found");
        } else {
            System.out.println("Optimal solution found!");
        }


        /* Print the solution */
//        System.out.println(op.getPrimalSolution("isOpen").toString());
        System.out.println("\nOptimal cost: " + op.getOptimalCost() + "\n");
//        System.out.println(op.getPrimalSolution("yPH"));
//        System.out.println(op.getPrimalSolution("yHH"));
        DoubleMatrixND yPC = op.getPrimalSolution("yPC");
        int numScal = op.getNumScalarDecisionVariables();
        System.out.println(op.getPrimalSolution("yPC"));
//        System.out.println(op.getPrimalSolution("yHC"));

        displayResults(producers, hubs, customers, op);
    }

    public static void displayResults(Producer[] producers, Hub[] hubs, Customer[] customers, OptimizationProblem op) {
        String[] results = op.getPrimalSolution("isOpen").toString().split(";;");
        int idx = 0;
        for (String res : results) {
            double open = Double.parseDouble(res);
            if (open == 1.0) {
                chosenHubs.add(idx);
                System.out.println(Arrays.deepToString(sumMatrix("yHC")));
                System.out.println(hubs[idx].getName() + " hub is OPEN\n");
            }
            idx++;
        }
        JFrame test = new JFrame("Google Maps");

        //Ajout des producteurs
        String mapString = "";
        for (int i = 1; i < producers.length; i++) {
            mapString += "&markers=color:blue%7Clabel:" + i + "%7C" + Double.toString(producers[i].getLongitude()) + "," + Double.toString(producers[i].getLatitude());
        }
        for (int i = 1; i < customers.length; i++) {
            mapString += "&markers=color:yellow%7Clabel:" + i + "%7C" + Double.toString(customers[i].getLongitude()) + "," + Double.toString(customers[i].getLatitude());
        }
        for (int i = 0; i < chosenHubs.size(); i++) {
            mapString += "&markers=color:red%7Clabel:" + i + "%7C" + Double.toString(hubs[chosenHubs.get(i)].getLongitude()) + "," + Double.toString(hubs[chosenHubs.get(i)].getLatitude());
        }

        try {
            String latitude = "45.1934574";
            String longitude = "5.7682659";
            String imageUrl = "https://maps.googleapis.com/maps/api/staticmap?center="
                    + latitude
                    + ","
                    + longitude
                    + "&zoom=9&size=1024x1024&scale=2&maptype=roadmap"
                    + mapString
                    + linkTwoPoints(sumMatrix("yPC"), "yPC", "0000ff");
            System.out.println(imageUrl);
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
        test.add(new JLabel(imageIcon));
// show the GUI window
        test.setVisible(true);
        test.pack();
    }

    private static void calculateShippingCosts(Producer[] producers, Hub[] hubs, Customer[] customers, int nbProduits, double[][][] cPH, double[][][] cHC, double[][][] cPC, double[][][] cHH, File excelFile) throws Exception {

        double costPtoC = ExcelTools.readCost(excelFile, "PtoC");
        double costHtoC = ExcelTools.readCost(excelFile, "HtoC");
        double costPtoH = ExcelTools.readCost(excelFile, "PtoH");
        double costHtoH = ExcelTools.readCost(excelFile, "HtoH");
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
                cost = (producers[i].getDistanceTo(hubs[j]) / 1000) * costPtoH * coefP;
                for (int l = 0; l < nbProduits; l++) {
                    cPH[i][j][l] = cost;
                }
            }
            for (int k = 0; k < customers.length; k++) {
                if (customers[i].getName().equals("Fiction")) {
                    coefC = 0;
                } else {
                    coefC = 1;
                }
                cost = (producers[i].getDistanceTo(customers[k]) / 1000) * costPtoC * coefC * coefP;
                for (int l = 0; l < nbProduits; l++) {
                    cPC[i][k][l] = cost;
                }

            }
        }

        for (int j = 0; j < hubs.length; j++) {
            for (int h = 0; h < hubs.length; h++) {
                cost = (hubs[j].getDistanceTo(hubs[h]) / 1000) * costHtoH;
                for (int l = 0; l < nbProduits; l++) {
                    cHH[j][h][l] = cost;
                }
            }
            for (int k = 0; k < customers.length; k++) {
                if (customers[k].getName().equals("Fiction")) {
                    coefC = 0;
                } else {
                    coefC = 1;
                }
                cost = (hubs[j].getDistanceTo(customers[k]) / 1000) * costHtoC * coefC;

                for (int l = 0; l < nbProduits; l++) {
                    cHC[j][k][l] = cost;
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

    private static String linkTwoPoints(int[][] matrix, String matrixType, String hexaColor) {
        String result = "";
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrixType.equals("yPC") && !producers[i].getName().equals("Fiction") && !customers[j].getName().equals("Fiction") && matrix[i][j]!=0) {
                        result += "&path=color:0x" + hexaColor + "%7Cweight:5%7C" + Double.toString(producers[i].getLongitude()) + "," + Double.toString(producers[i].getLatitude()) + "%7C" + Double.toString(customers[j].getLongitude()) + "," + Double.toString(customers[j].getLatitude());
                }
                if (matrixType.equals("yHC") && !customers[i].getName().equals("Fiction") && matrix[i][j]!=0) {
                        result += "&path=color:0x" + hexaColor + "%7Cweight:5%7C" + Double.toString(hubs[i].getLongitude()) + "," + Double.toString(hubs[i].getLatitude()) + "%7C" + Double.toString(customers[j].getLongitude()) + "," + Double.toString(customers[j].getLatitude());
                }
                if (matrixType.equals("yPH") && !producers[i].getName().equals("Fiction") && matrix[i][j]!=0) {
                        result += "&path=color:0x" + hexaColor + "%7Cweight:5%7C" + Double.toString(producers[i].getLongitude()) + "," + Double.toString(producers[i].getLatitude()) + "%7C" + Double.toString(hubs[j].getLongitude()) + "," + Double.toString(hubs[j].getLatitude());
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