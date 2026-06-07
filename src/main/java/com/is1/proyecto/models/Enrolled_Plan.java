package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@Table("enrolled_plan")
@CompositePK({ "student_id", "plan_id" })
public class Enrolled_Plan extends Model {
    
    public Integer getStudentId() { 
        return getInteger("student_id"); 
    }
    
    public void setStudentId(Integer id) { 
        set("student_id", id); 
    }
    
    public Integer getPlanId() { 
        return getInteger("plan_id"); 
    }
    
    public void setPlanId(Integer id) { 
        set("plan_id", id); 
    }

    /*Para la vista en academic-performance */
    public String getDisplayString() {
        Plan plan = parent(Plan.class);
        if (plan != null) {
            return plan.getDisplayString();
        }
        return "";
    }
}