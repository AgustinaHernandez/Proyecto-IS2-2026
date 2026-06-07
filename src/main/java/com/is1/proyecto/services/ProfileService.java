package com.is1.proyecto.services;

import java.util.List;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User;

/** Rutas --------------------------
 * /dashboard (GET), 
 * /set-role (POST), 
 * /profile (GET), 
 * /settings (GET), 
 * /profile/update-email (POST), 
 * /profile/verify-email (GET/POST).
 */

public class ProfileService {

    public static class ProfileDataResult {
        public boolean success;
        public String fullName;
        public Integer dni;
        public String email;
        public String degree;
    }

    // Método que concentra las búsquedas en la Base de Datos
    public static ProfileDataResult getProfileData(Long userId, String activeRole) {
        ProfileDataResult result = new ProfileDataResult();
        
        User currentUser = User.findById(userId);
        if (currentUser == null) {
            result.success = false;
            return result;
        }
        
        Person currentPerson = Person.findById(currentUser.get("person_id"));
        if (currentPerson == null) {
            result.success = false;
            return result;
        }

        result.success = true;
        result.fullName = currentPerson.getLastName() + ", " + currentPerson.getFirstName();
        result.dni = currentPerson.getInteger("dni");
        result.email = currentPerson.getString("email");

        // Si es profesor, buscamos su título
        if ("TEACHER".equals(activeRole)) {
            List<Teacher> teachers = Teacher.find("person_id = ?", currentPerson.getId());
            if (!teachers.isEmpty()) {
                result.degree = teachers.get(0).getDegree();
            }
        }
        
        return result;
    }

    

    //profile/verify-email    
    // Valida el formato del correo
    public static String validateNewEmail(String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return "El correo no puede estar vacío.";
        }
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if (!newEmail.matches(emailRegex)) {
            return "Formato de correo inválido.";
        }
        return null; // todo ok
    }

    //profile/update-email
    // Busca al usuario y actualiza su correo en la BD
    public static String updateEmailInDatabase(Object userIdAttr, String newEmail) {
        try {
            User user = User.findById(userIdAttr);
            if (user != null) {
                Person person = Person.findById(user.get("person_id"));
                if (person != null) {
                    person.set("email", newEmail);
                    person.saveIt();
                    return null; // todo ok
                }
            }
            return "No se encontró el perfil asociado a tu cuenta.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Ocurrió un error al intentar actualizar el correo en la base de datos.";
        }
    }
}