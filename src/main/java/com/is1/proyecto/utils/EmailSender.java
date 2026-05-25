package com.is1.proyecto.utils;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {
    private static final String REMITENTE = "sistdeinform@gmail.com"; 
    private static final String PASSWORD = "efwquobjmzanfgbm"; 

    public static void sendMail(String receiver, String title, String content) {
        CompletableFuture.runAsync(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(REMITENTE, PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(REMITENTE));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver));
                message.setSubject(title);
                message.setContent(content, "text/html; charset=utf-8");
                System.out.println("Enviando correo a "+receiver+"...");
                Transport.send(message);
                System.out.println("Correo enviado con éxito a "+receiver+"!");

            } catch (MessagingException e) {
                System.err.println("Error al enviar el correo: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static void sendRecoveryMail(String receiver, String code) {
        String title = "Código de recuperación de contraseña";
        
        String content = "<div style=\"font-family: Arial, sans-serif; color: #333333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eaeaea; border-radius: 8px;\">"
                + "<h2 style=\"color: #2563eb; margin-bottom: 20px;\">Recuperación de Acceso</h2>"
                + "<p style=\"font-size: 16px; line-height: 1.5;\">Hemos recibido una solicitud para restablecer la contraseña asociada a esta dirección de correo electrónico.</p>"
                + "<p style=\"font-size: 16px; line-height: 1.5;\">Ingresá el siguiente código de verificación de 6 dígitos en la aplicación para continuar con el proceso:</p>"
                + "<div style=\"background-color: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 6px; padding: 15px; margin: 25px 0; text-align: center;\">"
                + "<span style=\"font-family: monospace; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1f2937;\">" + code + "</span>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #6b7280; line-height: 1.5;\">Por razones de seguridad, no compartas este código con nadie.</p>"
                + "<p style=\"font-size: 14px; color: #6b7280; line-height: 1.5;\">Si no solicitaste este cambio, podés ignorar este correo de forma segura. Tu contraseña actual seguirá funcionando.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #eaeaea; margin: 30px 0;\">"
                + "<p style=\"font-size: 12px; color: #9ca3af; text-align: center;\">Este es un mensaje automático, por favor no respondas a este correo.</p>"
                + "</div>";

        sendMail(receiver, title, content);
    }

    public static void sendPasswordChangedWarning(String receiver){
        String title = "Aviso de seguridad: Tu contraseña ha sido cambiada";
        
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">\n" +
                    "    <div style=\"background-color: #f59e0b; padding: 20px; text-align: center;\">\n" +
                    "        <h2 style=\"color: #ffffff; margin: 0;\">Aviso de Seguridad</h2>\n" +
                    "    </div>\n" +
                    "    <div style=\"padding: 30px; color: #333333; background-color: #ffffff;\">\n" +
                    "        <p style=\"font-size: 16px; line-height: 1.5;\">Hola,</p>\n" +
                    "        <p style=\"font-size: 16px; line-height: 1.5;\">Te enviamos este correo para confirmarte que <strong>la contraseña de tu cuenta ha sido modificada exitosamente</strong>.</p>\n" +
                    "        <div style=\"background-color: #fffbeb; padding: 20px; border-radius: 6px; margin: 25px 0; border-left: 5px solid #ef4444;\">\n" +
                    "            <p style=\"margin: 0; font-size: 15px; color: #92400e; line-height: 1.5;\">\n" +
                    "                <strong>¿No fuiste vos?</strong><br>Si no solicitaste este cambio o creés que alguien accedió a tu cuenta sin autorización, por favor ponete en contacto con la administración del sistema inmediatamente.\n" +
                    "            </p>\n" +
                    "        </div>\n" +
                    "        <p style=\"font-size: 14px; color: #666666; margin-bottom: 0;\">Saludos cordiales,<br><strong>El equipo de Administración</strong></p>\n" +
                    "    </div>\n" +
                    "</div>";
                    
        sendMail(receiver, title, content);
    }

    public static void sendGenericAccountCreationMail(String receiver, String dniStr, String firstName, String lastName, String password) {
        String title = "Tus credenciales de acceso al sistema";
        
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">\n" +
                    "    <div style=\"background-color: #2563eb; padding: 20px; text-align: center;\">\n" +
                    "        <h2 style=\"color: #ffffff; margin: 0;\">¡Bienvenido al Sistema de Información!</h2>\n" +
                    "    </div>\n" +
                    "    <div style=\"padding: 30px; color: #333333; background-color: #ffffff;\">\n" +
                    "        <p style=\"font-size: 16px;\">Hola <strong>" + firstName + " " + lastName + "</strong>,</p>\n" +
                    "        <p style=\"font-size: 16px; line-height: 1.5;\">Tu cuenta ha sido creada con éxito. A continuación, te dejamos tus credenciales temporales de acceso:</p>\n" +
                    "        <div style=\"background-color: #f3f4f6; padding: 20px; border-radius: 6px; margin: 25px 0; border-left: 5px solid #2563eb;\">\n" +
                    "            <p style=\"margin: 0 0 10px 0; font-size: 16px;\"><strong>👤 Usuario (DNI):</strong> " + dniStr + "</p>\n" +
                    "            <p style=\"margin: 0; font-size: 16px;\"><strong>🔑 Contraseña:</strong> <span style=\"font-family: monospace; background: #e5e7eb; padding: 3px 8px; border-radius: 4px; font-size: 18px; letter-spacing: 1px;\">" + password + "</span></p>\n" +
                    "        </div>\n" +
                    "        <p style=\"font-size: 14px; color: #666666; background-color: #fffbeb; padding: 10px; border-left: 4px solid #f59e0b; border-radius: 4px;\">\n" +
                    "            ⚠️ <strong>Importante:</strong> Por cuestiones de seguridad, te pedimos que ingreses al sistema y cambies esta contraseña lo antes posible.\n" +
                    "        </p>\n" +
                    "        <br>\n" +
                    "        <p style=\"font-size: 14px; color: #666666; margin-bottom: 0;\">Saludos cordiales,<br><strong>El equipo de Administración</strong></p>\n" +
                    "    </div>\n" +
                    "</div>";

        sendMail(receiver, title, content);
    }

    public static void sendStudentRoleAddedMail(String receiver, String firstName, String lastName){
        String title = "Nuevo perfil habilitado en tu cuenta";
        
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;\">\n" +
                    "    <div style=\"background-color: #10b981; padding: 20px; text-align: center;\">\n" +
                    "        <h2 style=\"color: #ffffff; margin: 0;\">¡Nuevo perfil habilitado!</h2>\n" +
                    "    </div>\n" +
                    "    <div style=\"padding: 30px; color: #333333; background-color: #ffffff;\">\n" +
                    "        <p style=\"font-size: 16px;\">Hola <strong>" + firstName + " " + lastName + "</strong>,</p>\n" +
                    "        <p style=\"font-size: 16px; line-height: 1.5;\">Te informamos que se ha habilitado el perfil de <strong>Estudiante</strong> en tu cuenta institucional.</p>\n" +
                    "        <p style=\"font-size: 16px; line-height: 1.5;\">Puedes seguir ingresando al sistema con tu DNI y tu contraseña habitual. Una vez dentro, podrás usar el menú desplegable para alternar entre tus perfiles.</p>\n" +
                    "        <br>\n" +
                    "        <p style=\"font-size: 14px; color: #666666; margin-bottom: 0;\">Saludos cordiales,<br><strong>El equipo de Administración</strong></p>\n" +
                    "    </div>\n" +
                    "</div>";

        sendMail(receiver, title, content);
    }
}