package models;

import java.util.HashMap;
import java.util.Map;

public class Customer extends models.Location {
    private String cat;
    private Map<String, Integer> demand;

    public Customer(int noCustomer, String name, String cat, double longitude, double latitude, Map<String, Integer> demand) {
        super(noCustomer, name, longitude, latitude);
        this.cat = cat;
        this.demand = demand;
    }

    public Customer(int noCustomer, String name, String cat, double longitude, double latitude) {
        super(noCustomer, name, longitude, latitude);
        this.cat = cat;
        this.demand = new HashMap<String, Integer>();
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public Map<String, Integer> getDemand() {
        return demand;
    }

    public void setDemand(String key, int value) {
        assert value >= 0;
        assert !key.equals("");
        assert !this.demand.containsKey(key);
        this.demand.put(key, value);
    }
}