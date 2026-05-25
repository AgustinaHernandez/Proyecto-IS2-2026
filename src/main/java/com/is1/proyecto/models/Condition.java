package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@Table("conditions")
@CompositePK({ "subject_id", "correlative_id" })
public class Condition extends Model {

    public Subject getSubject() {
        return Subject.findById(get("subject_id"));
    }

    public Subject getCorrelative() {
        return Subject.findById(get("correlative_id"));
    }

    public String getCourseCondition() {
        return getString("course_condition");
    }

    public String getExamCondition() {
        return getString("exam_condition");
    }
}