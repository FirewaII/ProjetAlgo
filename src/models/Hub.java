package models;

public class Hub extends models.Location {
    private int opCost;

    public Hub(int noHub,String city, int opCost, double longitude, double latitude){
        super(noHub,city,longitude,latitude);
        assert opCost>0;
        this.opCost = opCost;
    }

    public void setOpCost(int opCost) {
        assert opCost>0;
        this.opCost = opCost;
    }

    public int getOpCost() {
        return opCost;
    }
}