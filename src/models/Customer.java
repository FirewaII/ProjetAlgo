package models;

import java.util.HashMap;
import java.util.Map;

public class Customer extends models.Location {
    private String cat;
    private Map<Integer,Map<String, Integer>> demand;


    public Customer(int noCustomer, String name, String cat, double longitude, double latitude) {
        super(noCustomer, name, longitude, latitude);
        this.cat = cat;
        this.demand = new HashMap<>();
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public Map<Integer,Map<String, Integer>> getDemand() {
        return demand;
    }

    public void setDemand(int period, String key, int value) {
        assert value >= 0;
        assert !key.equals("");

        if (!demand.containsKey(period)) {
            HashMap<String, Integer> subDemand = new HashMap<>();
            subDemand.put(key, value);
            this.demand.put(period, subDemand);
        } else {
            this.demand.get(period).put(key, value);
        }
    }
}