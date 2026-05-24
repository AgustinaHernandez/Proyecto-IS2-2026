package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.Table;

@Table("recover_password_codes")
@CompositePK({"user_id", "code"})
public class RecoverPasswordCode extends Model {}
