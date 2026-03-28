package com.is1.proyecto.models;                                             // Controlar

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@Table("enrolled_subject")
@CompositePK({ "student_id", "subject_id" })
public class Enrolled_Subject extends Model {
    
    public Integer getStudentId() { 
        return getInteger("student_id"); 
    }
    
    public void setStudentId(Integer id) { 
        set("student_id", id); 
    }
    
    public Integer getSubjectId() { 
        return getInteger("subject_id"); 
    }
    
    public void setSubjectId(Integer id) { 
        set("subject_id", id); 
    }
}