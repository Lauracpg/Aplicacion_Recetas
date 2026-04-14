package com.example.aplicacion_recetas;

public class Usuario {
    private int id;
    private String nombre;
    private String email;
    private String password;
    private String foto;

    public Usuario(String nombre, String email, String password, String foto) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.foto = foto;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getFoto() { return foto; }
}