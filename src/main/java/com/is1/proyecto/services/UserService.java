package com.is1.proyecto.services;

import com.is1.proyecto.models.User;

public class UserService {
    
    public static User find(Object id){
        return User.findById(id);
    }
}
