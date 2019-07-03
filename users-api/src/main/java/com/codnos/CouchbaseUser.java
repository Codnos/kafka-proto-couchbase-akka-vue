package com.codnos;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CouchbaseUser {
    private String id;

    private String name;

    private String favoriteColor;

    private List<String> salaryStructure;

    private Map<String, List<BigDecimal>> salaries;

    private Integer salaryPrecision;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFavoriteColor() {
        return favoriteColor;
    }

    public void setFavoriteColor(String favoriteColor) {
        this.favoriteColor = favoriteColor;
    }

    public Integer getSalaryPrecision() {
        return salaryPrecision;
    }

    public void setSalaryPrecision(Integer salaryPrecision) {
        this.salaryPrecision = salaryPrecision;
    }

    public List<String> getSalaryStructure() {
        return salaryStructure;
    }

    public void setSalaryStructure(List<String> salaryStructure) {
        this.salaryStructure = salaryStructure;
    }

    public Map<String, List<BigDecimal>> getSalaries() {
        return salaries;
    }

    public void setSalaries(Map<String, List<BigDecimal>> salaries) {
        this.salaries = salaries;
    }
}
