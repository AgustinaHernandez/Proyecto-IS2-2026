package com.is1.proyecto.models;                                               // Controlar

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("plans")
@IdName("id")
public class Plan extends Model {

    public Integer getID() {
        return getInteger("id");
    }

    public Integer getCareerId() {
        return getInteger("career_id");
    }

    public void setCareerId(Integer careerId) {
        set("career_id", careerId);
    }

    public Integer getVersion() {
        return getInteger("version");
    }

    public void setVersion(Integer version) {
        set("version", version);
    }
}