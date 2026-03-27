package com.is1.proyecto.models;

import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Many2Many;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teachers") // Esta anotación asocia explícitamente el modelo 'Teacher' con la tabla 'teacher' en la DB.
@Many2Many(other = Subject.class, join = "teaches", sourceFKName = "teacher_id", targetFKName = "subject_id")
@BelongsTo(parent = Person.class, foreignKeyName = "id")
public class Teacher extends Model {
    
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
    
// Atributos propios de Teacher

    public String getDegree(){
        return getString("degree");
    }

    public void setDegree(String degree){
        set("degree", degree);
    }

    public String getFullNameString() {
        return getFirstName() + " " + getLastName();
    }
}

