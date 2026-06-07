package com.is1.proyecto.utils;


public class InputValidator {
    public static String checkNoEmptyFields(String... fields){
        if (fields != null && fields.length > 0) {
            for (String field : fields) {
                if (field == null || field.trim().isEmpty()) {
                    return "Todos los campos son requeridos.";
                }
            }
        }
        return null;
    }

    public static String validateEmail(String email){
        if (email == null || email.isEmpty()) {
            return "Por favor, ingrese un correo electrónico.";
        }

        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if(!email.matches(emailRegex)) {
            return "Ingrese un correo electrónico válido (ej: usuario@dominio.com).";
        }
        return null;
    }

    public static String validateName(String name){
        String result = name.replaceAll("\\d", ""); //Quitar todos los números del name
        if(result.length() != name.length()){ //Chequear si cambió la longitud
            return "El nombre y/o apellido no puede contener números.";
        }
        return null;
    }

    public static String validateDNI(String dniString){
        Integer dni = 0;
        try {
            dni = Integer.parseInt(dniString);
            if (dni <= 0) throw new IllegalArgumentException("DNI inválido.");
        } catch (Exception e) {
            return "El DNI debe ser un número válido.";
        }
        return null;
    }
}
