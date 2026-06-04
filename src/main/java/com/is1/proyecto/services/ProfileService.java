package com.is1.proyecto.services;

import com.is1.proyecto.models.Person;
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