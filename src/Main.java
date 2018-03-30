
//import Tools.ExcelTools;

import Tools.ExcelTools;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import models.*;
import sun.misc.Unsafe;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

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
        File excelFile;
        try{
            excelFile = new File(args[0]);
        }
        catch (Exception e){
            System.out.println("Veuillez vérifier le paramètre saisi");
            return;
        }
//        File excelFile = new File("res/Projet_DistAgri_Inst_Petite.xlsx"); // Launching with IDE
//        File excelFile = new File("res/Projet_DistAgri_Inst_Moyenne.xlsx"); // Launching with IDE
//        File excelFile = new File("res/Projet_DistAgri_Inst_Grande.xlsx"); // Launching with IDE


        System.out.println("Adding locations...");
        /* Initialise variables */
        producers = ExcelTools.readProducers(excelFile);
        hubs = ExcelTools.readHubs(excelFile);
        customers = ExcelTools.readCustomers(excelFile);

        /*Vars*/
//        Random ran = new Random();
        int nbPeriodes = max(producers[1].getSupply().size(), customers[1].getDemand().size());
        int nbProduits = max(producers[1].getSupply().get(1).size(), customers[1].getDemand().get(1).size());

        /*Hubs opening cost matrix*/
        double[][] openCost = new double[hubs.length][1];
        // On utilise une matrice de taille hubs.length x 1 pour faciliter la multiplication dans la fonction objective du simplex
        for (int i = 0; i < hubs.length; i++) {
            // Pour chaque Hub , on ajoute son cout d'ouverture dans une ligne séparée
            openCost[i][0] = (double) hubs[i].getOpCost();
        }


        System.out.println("Calculating initial O/D...");
        /*Setting Offer / Demand, not including fictive OD*/
        double[][][] offer = new double[producers.length][nbPeriodes][nbProduits];
        double[][][] demand = new double[customers.length][nbPeriodes][nbProduits];
        calculateOffer(offer, 1);
        calculateDemand(demand, 1);

        /*Preparing initial OD to calculate fictive OD*/
        int[][] prodOffer = new int[nbPeriodes][nbProduits];
        // Pour chaque producer
        for (double[][] subOffer : offer) {
            // Pour chaque période
            for (int i = 0; i < nbPeriodes; i++) {
                // pour chaque produit
                for (int currentProd = 0; currentProd < nbProduits; currentProd++) {
                    // on somme la quantité dans la matrice prodOffer
                    prodOffer[i][currentProd] += subOffer[i][currentProd];
                }
            }

        }

        int[][] prodDemand = new int[nbPeriodes][nbProduits];
        // Pour chaque client
        for (double[][] subDemand : demand) {
            // pour chaque période
            for (int i = 0; i < nbPeriodes; i++) {
                // pour chaque produit
                for (int currentProd = 0; currentProd < nbProduits; currentProd++) {
                    // on somme la quantité dans la matrice prodDemand
                    prodDemand[i][currentProd] += subDemand[i][currentProd];
                }
            }
        }


        System.out.println("Adding fictive O/D...");
        /*Fictive offer/demand*/
        // !!!!! LE CLIENT/PRODUCER FICTIF EST TOUJOURS A L'INDICE 0 !!!!!
        // pour chaque période
        for (int i = 0; i < nbPeriodes; i++) {
            // Pour chaque produit
            for (int j = 0; j < nbProduits; j++) {
                // On calcule la difference entre la demande et l'offre
                int offset = prodOffer[i][j] - prodDemand[i][j];
                // Si la demande est supérieure à l'offre
                if (prodDemand[i][j] > prodOffer[i][j]) {
                    // On crée de l'offre fictive pour satisfaire le surplus de demande
                    producers[0].setSupply(i, "Produit fictif " + j, -offset);
                    // On oublie pas de créer egalement une valeur nulle dans la demande pour garder la meme taille de matrice
                    customers[0].setDemand(i, "Produit fictif " + j, 0);
                } else {
                    // On crée de la demande fictive pour satisfaire le surplus d'offre
                    customers[0].setDemand(i, "Produit fictif " + j, offset);
                    // On oublie pas de créer egalement une valeur nulle dans l'offre pour garder la meme taille de matrice
                    producers[0].setSupply(i, "Produit fictif " + j, 0);
                }
            }
        }

        /*Including fictive OD in the initial OD*/
        calculateOffer(offer, 0);
        calculateDemand(demand, 0);

        /*Shipping cost using distances*/
        /* Prod => Hub */
        double[][][][] cPH = new double[producers.length][hubs.length][nbPeriodes][nbProduits];
        /* Hub => Client */
        double[][][][] cHC = new double[hubs.length][customers.length][nbPeriodes][nbProduits];
        /* Prod => Client */
        double[][][][] cPC = new double[producers.length][customers.length][nbPeriodes][nbProduits];
        /* Hub => Hub */
        double[][][][] cHH = new double[hubs.length][hubs.length][nbPeriodes][nbProduits];


        System.out.println("Calculating shipping costs...");
        /*Calcul des couts de transport en fonction de la distance (km)*/
        /*Si le nombre de Producers, Hubs et Customers est petit, on utilise l'API de Google Maps*/
        calculateShippingCosts(producers, hubs, customers, nbPeriodes, nbProduits, cPH, cHC, cPC, cHH, excelFile);
        System.out.println("Costs calculated");


        /* Create the optimization problem object */
        op = new OptimizationProblem();


        System.out.println("Adding decision variables...");
        /* Add the decision variables to the problem */
        op.addDecisionVariable("isOpen", true, new int[]{hubs.length, 1}, 0, 1);  // name, isInteger, size , minValue, maxValue
        /*Nombre de produits à transferer,  P = Producer ; H = HUB ; C = Customer*/
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

        /*Cout de transfert,  P = Producer ; H = HUB ; C = Customer*/
        op.setInputParameter("cPH", new DoubleMatrixND(cPH));
        op.setInputParameter("cHC", new DoubleMatrixND(cHC));
        op.setInputParameter("cPC", new DoubleMatrixND(cPC));
        op.setInputParameter("cHH", new DoubleMatrixND(cHH));


        System.out.println("Generating constraints...");

        /* Add the constraints */

        /*produits sortants par hub == Somme des produits entrants par hub*/
        op.addConstraint("sum(yPH,1) - sum(yHC,2) == 0 ");

        /*Contrainte ouverture Hub: s'il existe un flux entre un producteur et un hub, peu importe la periode, le hub est alors considéré ouvert pendant toutes les periodes*/
        op.addConstraint("sum(sum(sum(yPH,4),3),1) <= M * sum(isOpen,1)");

        /*Contrainte ouverture Hub: s'il existe un flux entre un client et un hub, peu importe la periode, le hub est alors considéré ouvert pendant toutes les periodes*/
        op.addConstraint("sum(sum(sum(yHC,4),3),2) <= M * sum(isOpen,1)");

        /*Contrainte ouverture Hub: s'il existe un flux entre un hub et un autre, peu importe la periode, le hub est alors considéré ouvert pendant toutes les periodes*/
        op.addConstraint("sum(sum(sum(yHH,4),3),2) <= M * sum(isOpen,1)");
        op.addConstraint("sum(sum(sum(yHH,4),3),1) <= M * sum(isOpen,1)");

        /*produits entrants par client == Demande du client*/
        op.addConstraint("sum(yPC,1) + sum(yHC,1) == demand");

        /*produits sortants par producteur == Offre du producteur*/
        op.addConstraint("sum(yPC,2) + sum(yPH,2) == offer");

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
        System.out.println("\nOptimal cost: " + (int) op.getOptimalCost() + "\n");

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
            System.out.println("Affiche des résultats sur IHM");
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
                mapString += "&markers=icon:http://pierret.pro/F.png%7C" + Double.toString(producers[i].getLatitude()) + "," + Double.toString(producers[i].getLongitude());
            }
            for (int i = 1; i < customers.length; i++) {
                mapString += "&markers=icon:http://pierret.pro/C.png%7C" + Double.toString(customers[i].getLatitude()) + "," + Double.toString(customers[i].getLongitude());
            }
            for (int i = 0; i < chosenHubs.size(); i++) {
                mapString += "&markers=icon:http://pierret.pro/H.png%7C" + Double.toString(chosenHubs.get(i).getLatitude()) + "," + Double.toString(chosenHubs.get(i).getLongitude());
            }

            try {
                String imageUrl = "https://maps.googleapis.com/maps/api/staticmap?size=4096x4096&scale=2&maptype=roadmap"
                        + mapString
                        + linkTwoPoints(sumMatrix("yPC"), "yPC", "0000ff")
                        + linkTwoPoints(sumMatrix("yHC"), "yHC", "00ff00")
                        + linkTwoPoints(sumMatrix("yPH"), "yPH", "ff0000")
                        + linkTwoPoints(sumMatrix("yHH"), "yHH", "ff0000");
                String destinationFile = "image.jpg";
                // read the map image from Google
                // then save it to a local file: image.jpg
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
                System.exit(1);
            }
            // create a GUI component that loads the image: image.jpg
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

        double costPtoC = ExcelTools.readShippingCost(excelFile, "PtoC");
        double costHtoC = ExcelTools.readShippingCost(excelFile, "HtoC");
        double costPtoH = ExcelTools.readShippingCost(excelFile, "PtoH");
        double costHtoH = ExcelTools.readShippingCost(excelFile, "HtoH");
        int coefP; // Coef prod fictif
        int coefC; // Coef client fictif
        double cost;

        //Définir si on utilise l'API
        int nbPaths = customers.length * hubs.length + producers.length * hubs.length + producers.length * customers.length + hubs.length * hubs.length;

        useAPI = nbPaths <= 7500;
        if (useAPI) System.out.println("GMaps API Calls in progress, this might take a while...");
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
                    result += "&path=" + Double.toString(producers[i].getLatitude()) + "," + Double.toString(producers[i].getLongitude()) + "|" + Double.toString(customers[j].getLatitude()) + "," + Double.toString(customers[j].getLongitude());
                }
                if (matrixType.equals("yHC") && !customers[j].getName().equals("Fiction") && matrix[i][j] != 0 && chosenHubs.contains(hubs[i])) {
                    result += "&path=" + Double.toString(hubs[i].getLatitude()) + "," + Double.toString(hubs[i].getLongitude()) + "|" + Double.toString(customers[j].getLatitude()) + "," + Double.toString(customers[j].getLongitude());
                }
                if (matrixType.equals("yPH") && !producers[i].getName().equals("Fiction") && matrix[i][j] != 0 && chosenHubs.contains(hubs[j])) {
                    result += "&path=" + Double.toString(producers[i].getLatitude()) + "," + Double.toString(producers[i].getLongitude()) + "|" + Double.toString(hubs[j].getLatitude()) + "," + Double.toString(hubs[j].getLongitude());
                }
                if (matrixType.equals("yHH") && matrix[i][j] != 0 && chosenHubs.contains(hubs[i]) && chosenHubs.contains(hubs[j])) {
                    result += "&path=" + Double.toString(hubs[i].getLatitude()) + "," + Double.toString(hubs[i].getLongitude()) + "|" + Double.toString(hubs[j].getLatitude()) + "," + Double.toString(hubs[j].getLongitude());
                }
                if (result.length() > 8192) {
                    System.exit(1);
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