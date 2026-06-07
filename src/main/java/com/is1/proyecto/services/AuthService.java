package com.is1.proyecto.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.mindrot.jbcrypt.BCrypt;

import com.is1.proyecto.models.Enrolled_Plan;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.RecoverPasswordCode;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User;
import com.is1.proyecto.utils.EmailSender;

/** Rutas --------------------------
 *  /login (GET/POST), 
 *  /logout (GET), 
 *  /recover-password  (GET/POST), 
 *  /reset-password  (GET/POST), 
 *  /change-password  (GET/POST).
 */

public class AuthService {

    // (el método solo puede devolver una cosa, 
    // encapsulamos todo en una clase nueva, en lugar de usar un map).
    public static class LoginServiceResult {
        public boolean success;
        public int status;
        public String errorMessage;
        public User ac;
        public boolean isAdmin;
        public boolean isStudent;
        public boolean isTeacher;
        public boolean isRegularStudent;
        public int roleCount;
    }

    // POST: /login
    public static LoginServiceResult authenticate(String username, String plainTextPassword) {
        LoginServiceResult result = new LoginServiceResult();

        // Validaciones básicas: campos de usuario y contraseña no pueden ser nulos o vacíos.
        if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
            result.success = false;
            result.status = 400;
            result.errorMessage = "El nombre de usuario y la contraseña son requeridos.";
            return result;
        }

        // Busca la cuenta en la base de datos por el nombre de usuario.
        User ac = User.findFirst("name = ?", username);

        // Si no se encuentra ninguna cuenta con ese nombre de usuario.
        if (ac == null) {
            result.success = false;
            result.status = 401;
            result.errorMessage = "Usuario o contraseña incorrectos.";
            return result;
        }

        // Obtiene la contraseña hasheada almacenada en la base de datos.
        String storedHashedPassword = ac.getString("password");

        // Compara la contraseña en texto plano ingresada con la contraseña hasheada almacenada.
        // BCrypt.checkpw hashea la plainTextPassword con el salt de storedHashedPassword y compara.
        if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
            // Autenticación exitosa.
            result.success = true;
            result.status = 200;
            result.ac = ac;

            Integer personId = ac.getInteger("person_id");
            Integer isAdminInt = ac.getInteger("is_admin");
            result.isAdmin = isAdminInt != null && isAdminInt == 1;

            // -- Detectar roles -- 
            Student studentModel = Student.findFirst("person_id = ?", personId);
            result.isStudent = studentModel != null;
            result.isTeacher = Teacher.findFirst("person_id = ?", personId) != null;
            result.isRegularStudent = false;
            result.roleCount = (result.isAdmin ? 1 : 0) + (result.isStudent ? 1 : 0) + (result.isTeacher ? 1:0);

            if(result.isStudent){
                result.isRegularStudent = Enrolled_Plan.findFirst("student_id = ?", studentModel.getId()) != null;
            }

            return result;
        } else {
            // Contraseña incorrecta.
            result.success = false;
            result.status = 401;
            result.errorMessage = "Usuario o contraseña incorrectos.";
            return result;
        }
    }

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
