package com.example.jwt.dto;

public class SignUpRequest {
    private String name;
    private String email;
    private String password;
    private String address;
    private Double salary;

    public SignUpRequest() {
    }

    public SignUpRequest(String name, String email, String password, String address, Double salary) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.address = address;
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }
}
