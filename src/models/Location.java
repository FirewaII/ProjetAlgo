package models;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

class Location {
    private int noPlace;
    private String name;
    private double longitude;
    private double latitude;

    public Location(int noPlace, String name, double longitude, double latitude) {
        this.noPlace = noPlace;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public int getNoPlace() {
        return noPlace;
    }

    public void setNoPlace(int noPlace) {
        assert noPlace>0;
        this.noPlace = noPlace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the distance between the current location and another one
     * using the Google Maps DistanceMatrix API.
     * The returned value is an Integer and is expressed in meters.
     *
     * @param loc Location object to which the distance will be calculated
     * @return distance in meters
     */
    public long getDistanceTo(Location loc) throws Exception {
        assert loc != null;

        // Prep URL
        String origin = String.format("%s,%s", this.getLongitude(), this.getLatitude());
        String destination = String.format("%s,%s", loc.getLongitude(), loc.getLatitude());

        // Shortest path by time
        // https://maps.googleapis.com/maps/api/distancematrix/json?origins=45.17823,5.74396&destinations=45.21854,5.66133&key=AIzaSyBmJFq8fk7l0fA9cIUldb4Io7Prga1FmSc
        // Shortest path with alternatives
        // https://maps.googleapis.com/maps/api/directions/json?origin=45.17823,5.74396&destination=45.21854,5.66133&alternatives=true&key=AIzaSyBmJFq8fk7l0fA9cIUldb4Io7Prga1FmSc
        String apiUrl = "https://maps.googleapis.com/maps/api/directions/json";
        String apiKey = "AIzaSyBmJFq8fk7l0fA9cIUldb4Io7Prga1FmSc";
        String s = String.format("%s?origin=%s&destination=%s&alternatives=true&key=%s", apiUrl, origin, destination, apiKey);
        URL url = null;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Read from URL
        assert url != null;
        Scanner scan = null;
        try {
            scan = new Scanner(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String read = "";
        assert scan != null;
        while (scan.hasNext()) {
            read += scan.nextLine();
        }
        scan.close();

        // Build JSON
        JSONObject jsonObj = new JSONObject(read);
        if (!jsonObj.get("status").equals("OK")) {
            throw new Exception("API call failed");
        }


        // Get distance results
        long distanceResult = Long.MAX_VALUE;
        try {
            JSONArray routes = (JSONArray) jsonObj.get("routes");
            for (int i = 0; i < routes.length(); i++) {
                JSONObject subElem = (JSONObject) routes.get(i);
                JSONArray legs = (JSONArray) subElem.get("legs");
                subElem = (JSONObject) legs.get(0);
                JSONObject distance = (JSONObject) subElem.get("distance");
                if (distance.getInt("value") < distanceResult) {
                    distanceResult = distance.getInt("value");
                }
            }
            return distanceResult;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;

    }

    public long getDistanceFrom(Location loc) throws Exception {
        return loc.getDistanceTo(this);
    }

    public static void main(String[] args) throws Exception {
        Location l1 = new Location(1, "client1", 45.17823, 5.74396);
        Location l2 = new Location(2, "client2", 45.21854, 5.66133);

        System.out.println(l2.getDistanceTo(l1));
        System.out.println(l1.getDistanceFrom(l2));
        System.out.println(l2.getDistanceTo(l1));
        System.out.println(l1.getDistanceFrom(l2));

    }
}