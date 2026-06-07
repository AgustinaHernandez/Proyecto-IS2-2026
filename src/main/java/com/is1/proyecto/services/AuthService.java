package com.is1.proyecto.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.RecoverPasswordCode;
import com.is1.proyecto.models.User;
import com.is1.proyecto.utils.EmailSender;

/** Rutas --------------------------
 *  /login (GET/POST), 
 *  /logout (GET), 
 *  /recover-passwor  (GET/POST), 
 *  /reset-passwor  (GET/POST), 
 *  /change-passwor  (GET/POST).
 */

public class AuthService {
    // POST HANDLER: change-password
    public static String changePassword(Integer userId, String currentPassword, String newPassword, String confirmPassword) {
        //Validaciones iniciales
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return "Las contraseñas nuevas no coinciden.";
        }

        try {
            User user = User.findById(userId);
            if (user == null) {
                return "Usuario no encontrado.";
            }

            //Verificar contraseña actual
            String currentHash = user.getString("password");
            if (!org.mindrot.jbcrypt.BCrypt.checkpw(currentPassword, currentHash)) {
                return "La contraseña actual es incorrecta.";
            }

            //Hashear y guardar la nueva
            String newHash = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
            user.set("password", newHash);
            user.saveIt();

            //Mandar notificación por correo
            Object personId = user.get("person_id");
            Person person = Person.findById(personId);
            
            if (person != null) {
                String email = person.getString("email");
                if (email != null && !email.isEmpty()) {
                    EmailSender.sendPasswordChangedWarning(email);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return "Ocurrió un error interno al procesar el cambio.";
        }
    }

    //POST HANDLER: recover-password
    public static void recoverPassword(String email) {
        //Buscar a la persona por email en la tabla persons
        Person person = Person.findFirst("email = ?", email);
        //Si la persona no existe, lo ignoramos
        if (person != null) {
            //Buscar al usuario vinculado a esa persona
            User user = User.findFirst("person_id = ?", person.getId());
            if (user != null) {
                //Generar código random
                String rawCode = String.format("%06d", new java.util.Random().nextInt(999999));
                //Guardar código en la DB
                RecoverPasswordCode recovery = new RecoverPasswordCode();
                recovery.set("user_id", user.getId());
                recovery.set("code", rawCode);
                recovery.saveIt();
                //Enviar el mail de forma asíncrona
                try {
                    EmailSender.sendRecoveryMail(email,rawCode);
                    System.out.println("Correo enviado a: " + email + " con código: " + rawCode);
                } catch (Exception e) {
                    System.err.println("Error al enviar el correo:");
                    e.printStackTrace();
                }
            }
        }
    }

    //POST HANDLER: reset-password
    public static String resetPassword(String token, String newPassword){
        //Buscar el código en la DB
        RecoverPasswordCode recoveryRecord = RecoverPasswordCode.findFirst("code = ?", token);

        if (recoveryRecord == null) {
            String errorMsg = URLEncoder.encode("El código de verificación ingresado es incorrecto.", StandardCharsets.UTF_8);
            return errorMsg;
        }

        //Verificación de expiración del código (el límite es de 15 minutos)
        java.sql.Timestamp creationTime = recoveryRecord.getTimestamp("creation_time");
        long diferenciaMilisegundos = System.currentTimeMillis() - creationTime.getTime();
        long quinceMinutosEnMilisegundos = 15 * 60 * 1000;

        if (diferenciaMilisegundos > quinceMinutosEnMilisegundos) {
            recoveryRecord.delete(); // Eliminar el código expirado
            String errorMsg = URLEncoder.encode("El código expiró. Por favor, solicitá uno nuevo.", StandardCharsets.UTF_8);
            return errorMsg;
        }

        //Buscar el usuario asociado al código de recuperación
        Object userId = recoveryRecord.get("user_id");
        User user = User.findById(userId);

        if (user == null) {
            String errorMsg = URLEncoder.encode("No se pudo encontrar el usuario asociado.", StandardCharsets.UTF_8);
            return errorMsg;
        }

        //Actualización de contraseña e invalidación del token (en una transacción)
        try {
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
            user.set("password", hashedPassword);
            user.saveIt();

            recoveryRecord.delete();
            
            //Enviar mail de advertencia de contraseña cambiada
            Object personId = user.get("person_id");
            Person person = Person.findById(personId);
            String email = person.getMail();
            if (email != null && !email.isEmpty()) {
                EmailSender.sendPasswordChangedWarning(email);
            }

            System.out.println("Contraseña restablecida para el usuario ID: " + userId);
            return null;

        } catch (Exception e) {
            System.err.println("Error al actualizar la contraseña en la base de datos:");
            e.printStackTrace();
            String errorMsg = URLEncoder.encode("Ocurrió un error interno al procesar el cambio.", StandardCharsets.UTF_8);
            return errorMsg;
        }
    }
}
