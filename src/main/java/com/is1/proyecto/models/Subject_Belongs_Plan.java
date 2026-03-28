package com.is1.proyecto.models;                                            // Controlar

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@Table("subject_belongs_plan")                        
@CompositePK({ "plan_id", "subject_id" })
public class Subject_Belongs_Plan extends Model {

    
    public Integer getSubjectId() { 
        return getInteger("subject_id"); 
    }

    public void setSubjectId(Integer id) { 
        set("subject_id", id); 
    }

    public Integer getPlanId() { 
        return getInteger("plan_id"); 
    }
    
    public void setPlanId(Integer id) { 
        set("plan_id", id); 
    }

    
}