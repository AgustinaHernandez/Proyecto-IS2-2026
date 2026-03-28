package com.is1.proyecto.models;                                            // Controlar

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("careers")
@IdName("id")
public class Career extends Model {

    public Integer getID() {
        return getInteger("id");
    }

    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        set("name", name);
    }
}