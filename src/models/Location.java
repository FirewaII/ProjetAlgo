package models;

class Location {
    private int noPlace;
    private String name;
    private double longitude;
    private double latitude;

    public Location(int noPlace, String name, double longitude, double latitude){
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
}