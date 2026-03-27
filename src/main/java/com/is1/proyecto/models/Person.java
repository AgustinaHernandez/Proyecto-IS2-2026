package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("persons")
public class Person extends Model{

    public String getFirstName() {
        return getString("first_name"); // Obtiene el valor de la columna 'name'
    }

    public void setFirstName(String name) {
        set("first_name", name);
    }

    public String getLastName() {
        return getString("last_name");
    }

    public void setLastName(String lastname) {
        set("last_name", lastname);
    }

    public Integer getDNI(){
        return getInteger("dni");
    }

    public Integer getID(){
        return getInteger("id");
    }

    public void setDNI(int dni) {
        set("dni", dni);
    }

    public String getMail(){
        return getString("mail");
    }

    public void setMail(String mail){
        set("mail", mail);
    }
    
}
