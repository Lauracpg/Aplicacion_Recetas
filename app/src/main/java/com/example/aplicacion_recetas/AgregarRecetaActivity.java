package com.example.aplicacion_recetas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class AgregarRecetaActivity extends AppCompatActivity {
    EditText editTextTitulo, editTextCategoria, editTextTiempo, editTextIngredientes, editTextPasos;
    Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_receta);

        editTextTitulo = findViewById(R.id.editTextTitulo);
        editTextCategoria = findViewById(R.id.editTextCategoria);
        editTextTiempo = findViewById(R.id.editTextTiempo);
        editTextIngredientes = findViewById(R.id.editTextIngredientes);
        editTextPasos = findViewById(R.id.editTextPasos);
        btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("titulo", editTextTitulo.getText().toString());
            data.putExtra("categoria", editTextCategoria.getText().toString());
            data.putExtra("tiempo", Integer.parseInt(editTextTiempo.getText().toString()));
            data.putExtra("ingredientes", editTextIngredientes.getText().toString());
            data.putExtra("pasos", editTextPasos.getText().toString());

            setResult(RESULT_OK, data);
            finish();
        });

    }

}
