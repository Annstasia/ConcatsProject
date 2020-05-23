package com.example.concatsproject;

public class Contact
{
    private String id;
    private String name  = "";
    private String phone = "";

    public Contact(String name, String phone){
        this.name = name;
        this.phone = phone;
    }

    public Contact(){

    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}