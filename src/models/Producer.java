package models;
import java.util.Map;

public class Producer extends models.Location {
    Map<String, Integer> supply;

    public Producer(int noProd, String name, double longitude, double latitude, Map<String, Integer> supply){
        super(noProd,name,longitude,latitude);
        this.supply = supply;
    }

    public Map<String, Integer> getSupply() {
        return supply;
    }

    public void setSupply(Map<String, Integer> supply) {
        this.supply = supply;
    }
}