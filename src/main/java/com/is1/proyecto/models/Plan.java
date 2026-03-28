<<<<<<< HEAD
package com.is1.proyecto.models;                                               // Controlar

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("plans")
@IdName("id")
@BelongsTo(parent = Career.class, foreignKeyName = "career_id")
public class Plan extends Model {

     public String getDisplayString(){
        Career c = parent(Career.class);
        String careerName = (c != null) ? c.getString("name"):"Carrera desconocida";
        return careerName + " (Versión " + getString("version") + ")";
    
    
=======
package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;

@Table("plans")
@BelongsTo(parent = Career.class, foreignKeyName = "career_id")
public class Plan extends Model {
    public String getDisplayString(){
        Career c = parent(Career.class);
        String careerName = (c != null) ? c.getString("name"):"Carrera desconocida";
        return careerName + " (Versión " + getString("version") + ")";
>>>>>>> 9a5ce9f75b501df20df416fb687d7a3c0a76aeb0
    }
}