package com.is1.proyecto.models;             // Laburando

import java.util.ArrayList;
import java.util.List;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

@Table("students") // Igual que en 'Teacher'
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

    /**
     *  Devuelve las materias disponibles para inscribirse 
     */
    public List<Subject> getAvailableSubjectsToEnroll() {
        // 1. Obtener el plan actual del alumno a través de la tabla intermedia enrolled_plan
        // Nota: Si usás una clase modelo EnrolledPlan, podés cambiar esto. Si no, usamos SQL crudo por eficiencia:
        String planSql = "SELECT plan_id FROM enrolled_plan WHERE student_id = ? LIMIT 1";
        Object planIdObj = Base.firstCell(planSql, this.getId());
        
        List<Subject> available = new ArrayList<>();
        if (planIdObj == null) return available; // Si no tiene plan, no ve materias

        // Traemos todas las materias pertenecientes a ese Plan
        String subjectsSql = "SELECT s.* FROM subjects s " +
                             "JOIN subject_belongs_plan sbp ON s.id = sbp.subject_id " +
                             "WHERE sbp.plan_id = ?";
        List<Subject> allPlanSubjects = Subject.findBySQL(subjectsSql, planIdObj);

        // Filtramos las materias validas
        for (Subject subject : allPlanSubjects) {
            List<String> errors = this.validateEnrollment(subject);
            if (errors.isEmpty()) {
                available.add(subject);
            }
        }
        return available;
    }

    /**
     * Valida si el estudiante cumple con los requisitos para cursar una materia.
     * Retorna una lista con los motivos de rechazo. Si está vacía, puede inscribirse.
     */
    public List<String> validateEnrollment(Subject subject) {
        List<String> errors = new ArrayList<>();

        // Valida que no esté inscripto o cursando actualmente en el año vigente (tabla statuses)
        String currentEnrollmentSql = "SELECT COUNT(*) FROM statuses s " +
                                      "JOIN grade_sheets gs ON s.grade_sheet_id = gs.id " +
                                      "WHERE s.student_id = ? AND gs.subject_id = ? AND s.final_condition = 'INSCRIPTO'";
        Object currentCount = Base.firstCell(currentEnrollmentSql, this.getId(), subject.getId());
        if (currentCount != null && ((Number) currentCount).longValue() > 0) {
            errors.add("Ya te encuentras cursando o inscripto en esta materia.");
            return errors; 
        }

        // Valida que no la haya aprobado (en FinalSheet)
        if (hasPassedFinalExam(subject.getId())) {
            errors.add("Ya has aprobado el examen final de esta materia.");
            return errors;
        }

        // Obtener y verificar las correlativas exigidas
        List<Condition> conditions = Condition.where("subject_id = ?", subject.getId());

        for (Condition cond : conditions) {
            Object correlativeId = cond.get("correlative_id");
            String requiredStatus = cond.getString("course_condition");
            
            Subject prereq = Subject.findById(correlativeId);
            String prereqName = (prereq != null) ? prereq.getString("name") : "Materia correlativa";

            boolean isRegular = hasStatus(correlativeId, "REGULAR") || hasStatus(correlativeId, "APROBADO") || hasPassedFinalExam(correlativeId);
            boolean isApproved = hasStatus(correlativeId, "APROBADO") || hasPassedFinalExam(correlativeId);

            if (requiredStatus.equals("REGULAR") && !isRegular) {
                errors.add("Debes tener REGULAR la materia: " + prereqName + ".");
            } else if (requiredStatus.equals("APROBADA") && !isApproved) {
                errors.add("Debes tener APROBADA la materia: " + prereqName + ".");
            }
        }

        return errors;
    }

    /**
     * Busca en las actas de cursado (GradeSheets) si alcanzó una condición final específica.
     */
    private boolean hasStatus(Object subjectId, String targetStatus) {
        String sql = "SELECT COUNT(*) FROM statuses s " +
                     "JOIN grade_sheets gs ON s.grade_sheet_id = gs.id " +
                     "WHERE s.student_id = ? AND gs.subject_id = ? AND s.final_condition = ?";
        
        Object result = Base.firstCell(sql, this.getId(), subjectId, targetStatus);
        return result != null && ((Number) result).longValue() > 0;
    }

    /**
     * Busca en las actas de exámenes finales si tiene la materia aprobada (nota entre 4 y 10).
     */
    private boolean hasPassedFinalExam(Object subjectId) {
        String sql = "SELECT COUNT(*) FROM final_grades fg " +
                     "JOIN final_sheets fs ON fg.final_sheet_id = fs.id " +
                     "WHERE fg.student_id = ? AND fs.subject_id = ? AND fg.grade >= 4";
                     
        Object result = Base.firstCell(sql, this.getId(), subjectId);
        return result != null && ((Number) result).longValue() > 0;
    }


    
// Atributos propios de Student

    //Student no tiene atributos propios distintos de Person, no tiene sentido

    public String getFullNameString() {
        return getFirstName() + " " + getLastName();
    }
}
