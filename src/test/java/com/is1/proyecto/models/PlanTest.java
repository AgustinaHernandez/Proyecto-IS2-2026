package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class PlanTest {

    @BeforeAll
    public static void abrirConexion() {
        if (!Base.hasConnection()) {
            Base.open("org.sqlite.JDBC", "jdbc:sqlite:./target/test.db", "", "");
        }
    }

    @AfterAll
    public static void cerrarConexion() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @BeforeEach
    public void iniciarTransaccion() {
        Base.openTransaction();
    }

    @AfterEach
    public void deshacerTransaccion() {
        Base.rollbackTransaction(); 
    }



    // ----------- Tests --------------------


    
    @Test
    @DisplayName("Alta de Plan: Debería crear un plan correctamente")
    public void testAltaDePlan() {
        // Arrange
        Career carrera = new Career();
        carrera.set("name", "Ingeniería en Sistemas");
        carrera.saveIt();

        // Act
        Plan nuevoPlan = new Plan();
        nuevoPlan.set("career_id", carrera.getId());
        nuevoPlan.set("version", 2026);
        nuevoPlan.set("state", "VIGENTE");
        
        boolean guardadoExitoso = nuevoPlan.saveIt();

        // Assert
        assertTrue(guardadoExitoso, "El plan debería guardarse sin errores");
        assertNotNull(nuevoPlan.getId(), "El plan debería tener un ID autogenerado");
        
        // Verificación
        Plan planGuardado = Plan.findById(nuevoPlan.getId());
        assertEquals("VIGENTE", planGuardado.getString("state"));
        assertEquals(2026, planGuardado.getInteger("version"));
    }

    @Test
    @DisplayName("Actualización de Plan: Debería cambiar de VIGENTE a SUSPENDIDO")
    public void testActualizarPlan() {
        // Arrange
        Career carrera = new Career();
        carrera.set("name", "Abogacía");
        carrera.saveIt();

        Plan plan = new Plan();
        plan.set("career_id", carrera.getId());
        plan.set("version", 2020);
        plan.set("state", "VIGENTE");
        plan.saveIt();

        // Act
        plan.set("state", "SUSPENDIDO");
        boolean actualizado = plan.saveIt();

        // Assert
        assertTrue(actualizado, "El plan debería actualizarse correctamente");
        
        Plan planActualizado = Plan.findById(plan.getId());
        assertEquals("SUSPENDIDO", planActualizado.getString("state"), "El estado en la DB debería ser SUSPENDIDO");
    }
}