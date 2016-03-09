package com.fydp.retailxp.client;

/**
 * Created by dmok on 02/02/16.
 */
public class Shoe {
    // Members
    private String name;
    private double price;
    private Integer imageRes;

    // Constructors
    public Shoe() {
        this.name = "";
        this.price = 0;
        this.imageRes = null;
    }

    public Shoe(String name, double price, Integer imageRes) {
        this.name = name;
        this.price = price;
        this.imageRes = imageRes;
    }

    // Accessors
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return this.price; }
    public void setPrice(double price) { this.price = price; }

    public Integer getImageRes() { return this.imageRes; }
    public void setImageRes(Integer imageRes) { this.imageRes = imageRes; }
}
