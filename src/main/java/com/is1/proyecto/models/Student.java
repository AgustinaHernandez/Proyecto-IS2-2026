package com.is1.proyecto.models;

import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("students") 
@BelongsTo(parent = Person.class, foreignKeyName = "id")
public class Student extends Model {
    
    private Person persona;

    public Person getPerson(){
        if(persona == null){
            persona = parent(Person.class);
        }
        return persona;
    }

    
    public String getFirstName() {
        return getPerson().getString("first_name"); // Obtiene el valor de la columna 'name'
    }

    
    public void setFirstName(String name) {
        getPerson().set("first_name", name);
    }

    
    public String getLastName() {
        return getPerson().getString("last_name");
    }

    
    public void setLastName(String lastname) {
        getPerson().set("last_name", lastname);
    }

    
    public Integer getDNI(){
        return getPerson().getInteger("dni");
    }

    
    public void setDNI(int dni) {
        getPerson().set("dni", dni);
    }

    public String getFullNameString() {
        return getFirstName() + " " + getLastName();
    }
}
