package com.is1.proyecto.models;                               // Controlar

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@Table("teaches")
@CompositePK({ "teacher_id", "subject_id" })
public class Teach extends Model {
    
    public Integer getTeacherId() { 
        return getInteger("teacher_id"); 
    }
    
    public void setTeacherId(Integer id) { 
        set("teacher_id", id); 
    }
    
    public Integer getSubjectId() { 
        return getInteger("subject_id"); 
    }
    
    public void setSubjectId(Integer id) { 
        set("subject_id", id); 
    }
}