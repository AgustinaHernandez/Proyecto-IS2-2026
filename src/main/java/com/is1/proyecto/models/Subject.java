package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Many2Many;
import org.javalite.activejdbc.annotations.Table;

@Table("subjects")
@IdName("id")
@Many2Many(other = Plan.class, join = "subject_belongs_plan", sourceFKName = "subject_id", targetFKName = "plan_id")
public class Subject extends Model {

    public Integer getID() {
        return getInteger("id");
    }

    public String getName() {
        return getString("name");
    }

    public Integer getResponsibleId() {
        return getInteger("responsible_id");
    }

    public void setResponsibleId(Integer teacherId) {
        set("responsible_id", teacherId);
    }

}