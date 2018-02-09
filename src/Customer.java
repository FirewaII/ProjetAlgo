class Customer extends Place{
    private String cat;
    private Map<String, Integer> demand;

    public Customer(int noCustomer, String name, String cat, double longitude, double latitude, Map<String,Integer> demand){
        super(noCustomer,name,longitude,latitude);
        this.cat = cat;
        this.demand = demand;
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

    public void setDemand(Map<String, Integer> demand) {
        this.demand = demand;
    }
}