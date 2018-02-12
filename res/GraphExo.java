/* Poser S={0} , l'ensemble des sommets visités
 lambda(0) = 0 ; lambda(i) = | Voi si (O, i) € U
                                sinon +inf                     pour le sommet 0: lambda(1) = 4, lambda(2) = 1, lambda(3) = 3
                                
Tant que S != X
    - Choisir sommet i € X \S de lambda(i) minimal
    - poser S = S U {i}
    - pour tout sommet j € (X\S) intersect T+(i)
        Calculer lambda(j) = min(lambda(i) + Vij, lambda(j))
*/

/**
 * Graph
 */
public class Graph {
    int nbSommets;
    SommetArcs[] vectSommets;
    int nbSommetsVisités;
    boolean[] sommetsVisités;
    int[] pccVal;
    int[] predecesseurs;

    Graph(Datafile f) {
        f.openFile();
        nbSommets = f.readint();
        vectSommets = new SommetArcs[nbSommets];
        for (int i = 0, i<nbSommets, i++){
            vectSommets[i] = new SommetArcs(i, nbSommets, f);
        }
        nbSommetsVisités = 0;
        sommetsVisités = new boolean[nbSommets];
        pccVal = new int[nbSommets];
        predecesseurs = new int[nbSommets];
        for (int i = 0, i<nbSommets, i++){
            sommetsVisités[i] = false;
            pccVal[i] = 9999; // On considere que c'est le max
            predecesseurs[i] = -1;
        }
        f.closeFile();
    }

    // Algo Dijkstra
    void pcc() {
        int i;
        int indMin;

        i = 0;
        sommetsVisités[0] = true;
        pccVal[0] = 0;

        for (int j = 1, j<nbSommets, j++){
            if (vectSommets[0].getSommetsArcs()[j] < 9999) {
                pccVal[j] = vectSommets[0].getSommetsArcs[j];
                predecesseurs[j] = 0;
            }
        }

        indMin = 0;
        while (i < nbSommets && indMin != -1) {
            indMin = minPCCVal();
            if (indMin != -1) {
                sommetsVisités[indMin] = true;
                for (int j = 0, j<nbSommets, j++){
                    if (!sommetsVisités[j]) {
                        if (vectSommets[indMin].getSommetsArcs[j] < 9999) {
                            if (pccVal[j] > pccVal[indMin] + vectSommets[indMin].getSommetsArcs[j]) {
                                //  lambdaJ         lambdaI       Vij
                                pccVal[j] = pccVal[indMin] + vectSommets[indMin].getSommetsArcs[j]
                                predecesseurs[j] = indMin;
                            }
                        }

                    }
                }

            }

        }
    }
}

class Executable {
    public static void main(String[] args) {
        Graph G;
        G = new Graph("Datafile");
        G.display();
        G.pcc();
        G.displaySol();
    }
}


