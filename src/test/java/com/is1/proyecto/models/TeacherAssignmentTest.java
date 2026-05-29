package com.is1.proyecto.models;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class TeacherAssignmentTest {

    @BeforeAll
    public static void abrirConexion() {
        if (!Base.hasConnection())
            Base.open("org.sqlite.JDBC", "jdbc:sqlite:./target/test.db", "", "");
    }

    @AfterAll
    public static void cerrarConexion() {
        if (Base.hasConnection())
            Base.close();
    }

    @BeforeEach
    public void iniciarTransaccion() {
        Base.openTransaction();
    }

    @AfterEach
    public void deshacerTransaccion() {
        Base.rollbackTransaction(); 
    }

    // metodo auxiliar para usar en los tests
    private Teacher crearProfesor(int dni, int fileCode) {
        Person p = new Person();
        p.set("dni", dni);
        p.set("first_name", "Profe");
        p.set("last_name", "Test");
        p.saveIt();

        Teacher t = new Teacher();
        t.set("person_id", p.getId());
        t.set("file_code", fileCode);
        t.set("degree", "PROFESORADO");
        t.saveIt();
        
        return t;
    }

    private Subject crearMateria(int code, String name, Integer responsibleId) {
        Subject s = Subject.createIt(
            "code", code, 
            "name", name, 
            "responsible_id", responsibleId
        );
        return s;
    }


    // --- TESTS ---

    @Test
    @DisplayName("Asignación: Un profesor se asigna a la tabla teaches")
    public void testAsignarProfesor() {
        // Arrange
        Teacher titular = crearProfesor(11111111, 100);
        Teacher ayudante = crearProfesor(22222222, 101);

        Subject materia = crearMateria(1001, "Algoritmos", titular.getInteger("id"));
        materia.saveIt();

        // Act
        materia.add(ayudante); 
        
        // Assert
        long cantidad = Base.count("teaches", "teacher_id = ? AND subject_id = ?", ayudante.getId(), materia.getId());
        assertEquals(1, cantidad, "Debe existir 1 registro de asignación en la tabla teaches");
    }


    @Test
    @DisplayName("Desasignación: Un profesor colaborador se puede desasignar")
    public void testDesasignarProfesorColaborador() {
        // Arrange
        Teacher titular = crearProfesor(33333333, 102);
        Teacher ayudante = crearProfesor(44444444, 103);

        Subject materia = crearMateria(1002, "Sistemas Operativos", titular.getInteger("id"));

        materia.add(ayudante); // Lo asignamos

        // Act
        int rowsDeleted = Base.exec("DELETE FROM teaches WHERE teacher_id = ? AND subject_id = ?", ayudante.getId(), materia.getId());
        
        // Assert
        assertEquals(1, rowsDeleted, "Se debería haber eliminado exactamente 1 registro");
        long cantidadRestante = Base.count("teaches", "teacher_id = ? AND subject_id = ?", ayudante.getId(), materia.getId());
        assertEquals(0, cantidadRestante, "El profesor ya no debería figurar en la materia");
    }


    @Test
    @DisplayName("Regla de Negocio: Detectar si el profesor a desasignar es el responsable")
    public void testEvitarDesasignarResponsable() {
        // Arrange
        Teacher titular = crearProfesor(55555555, 104);
        Subject materia = crearMateria(1003, "Bases de Datos", titular.getInteger("id"));
        materia.add(titular);

        // Act
        Integer idProfesorAQuitar = titular.getInteger("id");
        Integer idResponsableMateria = materia.getInteger("responsible_id");
        
        boolean esElResponsable = idResponsableMateria.equals(idProfesorAQuitar);
        
        // Assert
        assertTrue(esElResponsable, "La lógica debe detectar que es el responsable para poder abortar la operación");
    }
}
