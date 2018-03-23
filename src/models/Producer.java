package models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Producer extends models.Location {
    private Map<Integer, Map<String, Integer>> supply;

    public Producer(int noProd, String name, double longitude, double latitude) {
        super(noProd, name, longitude, latitude);
        this.supply = new HashMap<>();
    }

    public Map<Integer, Map<String, Integer>> getSupply() {
        return supply;
    }

    public void setSupply(int period, String key, int value) {
        assert value >= 0;
        assert !key.equals("");

        if (!supply.containsKey(period)) {
            HashMap<String, Integer> subSupply = new HashMap<>();
            subSupply.put(key, value);
            this.supply.put(period, subSupply);
        } else {
            this.supply.get(period).put(key, value);
        }

    }
}