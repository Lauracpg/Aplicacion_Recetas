package com.example.aplicacion_recetas;

import java.io.Serializable;

public class Receta implements Serializable {
    public int id;
    public String titulo;
    public String categoria;
    public int tiempo; //mins
    public String ingredientes;
    public String pasos;
    public String fotoUri;
}
