package bot.models;

public class Car {
    private int id;
    private String name;
    private double pricePerHour;
    private double pricePerDay;
    private String picture;
    private String time;
    private long managerId;

    private boolean avaliable;

    public Car(int id, String name, double pricePerHour, double pricePerDay, String picture,boolean avaliable,String time,
               long managerId) {
        this.id = id;
        this.name = name;
        this.pricePerHour = pricePerHour;
        this.pricePerDay = pricePerDay;
        this.picture = picture;
        this.avaliable = avaliable;
        this.time = time;
        this.managerId = managerId;
    }
    public Car(){}

    public long getManagerId() {
        return managerId;
    }

    public void setManagerId(long managerId) {
        this.managerId = managerId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isAvaliable() {
        return avaliable;
    }

    public void setAvaliable(boolean avaliable) {
        this.avaliable = avaliable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
