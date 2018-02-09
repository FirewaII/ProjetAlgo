package models;

class Hub extends Location{
    int opCost;

    public Hub(int noHub,String city, int opCost, double longitude, double latitude){
        super(noHub,city,longitude,latitude);
        this.opCost = opCost;
    }

    public void setOpCost(int opCost) {
        this.opCost = opCost;
    }

    public int getOpCost() {

        return opCost;
    }
}