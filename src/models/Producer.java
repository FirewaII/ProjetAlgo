package models;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Producer extends models.Location {
    private Map<String, Integer> supply;

    public Producer(int noProd, String name, double longitude, double latitude, Map<String, Integer> supply){
        super(noProd,name,longitude,latitude);
        this.supply = supply;
    }

    public Producer(int noProd, String name, double longitude, double latitude){
        super(noProd,name,longitude,latitude);
        this.supply = new HashMap<String, Integer>();
    }

    public Map<String, Integer> getSupply() {
        return supply;
    }

    public void setSupply(String key, int value) {
        assert value >= 0;
        assert !key.equals("");
        assert !this.supply.containsKey(key);
        this.supply.put(key, value);
    }
}