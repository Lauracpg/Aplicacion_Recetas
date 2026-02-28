package com.example.aplicacion_recetas;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DetalleRecetaActivity extends AppCompatActivity {
    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_receta);

        Receta receta = (Receta) getIntent().getSerializableExtra("receta");

        DetalleRecetaFragment fragment = (DetalleRecetaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);

        if(fragment != null && receta != null) {
            fragment.mostrarReceta(receta);
        }
    }
}
