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
     * @param  loc  Location object to which the distance will be calculated
     * @return      distance in meters
     */
    public int getDistance(Location loc) throws Exception {
        assert loc != null;

        // Prep URL
        String originLat = String.valueOf(this.getLatitude());
        String originLong = String.valueOf(this.getLongitude());
        String origin = String.format("%s,%s", originLong, originLat);

        String destLat = String.valueOf(loc.getLatitude());
        String destLong = String.valueOf(loc.getLongitude());
        String destination = String.format("%s,%s", destLong, destLat);

        // https://maps.googleapis.com/maps/api/distancematrix/json?origins=45.17823,5.74396&destinations=45.21854,5.66133&key=AIzaSyBmJFq8fk7l0fA9cIUldb4Io7Prga1FmSc
        String apiUrl = "https://maps.googleapis.com/maps/api/distancematrix/json";
        String apiKey = "AIzaSyBmJFq8fk7l0fA9cIUldb4Io7Prga1FmSc";
        String s = String.format("%s?origins=%s&destinations=%s&key=%s", apiUrl, origin, destination, apiKey);
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

        String read = new String();
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


        // Get distance result
        try {
            JSONArray rows = (JSONArray) jsonObj.get("rows");
            JSONObject subElem1 = (JSONObject) rows.get(0);
            JSONArray elements = (JSONArray) subElem1.get("elements");
            JSONObject subElem2 = (JSONObject) elements.get(0);
            JSONObject distance = (JSONObject) subElem2.get("distance");
            return distance.getInt("value");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;

    }

    public static void main(String[] args) {
        Location l1 = new Location(1, "client1", 45.17823, 5.74396);
        Location l2 = new Location(2, "client2", 45.21854, 5.66133);
        try {
            System.out.println(l1.getDistance(l2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}