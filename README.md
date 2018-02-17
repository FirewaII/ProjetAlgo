# ProjetAlgo

## GLPK

### Windows
Copy glpk.dll located in ProjetAlgo/res/ to C:\Windows\System32

### Linux
 In some Linux distibutions, GLPK is automatically installed.<br />
 Check if you can find the file libglpk.so in your system, usually in /usr/local/lib/libglpk.so. If so, you already have what you need.

Otherwise
<pre>
$ sudo apt-get install glpk
</pre>


## Modelisation
```
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
```


