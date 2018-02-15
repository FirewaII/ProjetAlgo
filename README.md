# ProjetAlgo



// Produits transférés d'un endroit à un autre par produit par période
y[farm][hub][product][period] 
y[hub][client][product][period] 


// Cout de transport d'une unité de produit d'un endroit à un autre
shipCost[farm][hub] = (int) 1 * distance[farm][hub] / 1000
shipCost[hub][client] = (int) 0,5 * distance[hub][client] / 1000


// isOpen[hub] == 1 si Hub h est ouvert
isOpen[hub][period]

// Cout d'ouverture d'un hub
openCost[hub]


// 
