package com.example.aplicacion_recetas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    DBHelper db;
    Button btnAgregar;
    TextView textViewRecetas;
    ActivityResultLauncher<Intent> startForResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        btnAgregar = findViewById(R.id.btnAgregarReceta);
        textViewRecetas = findViewById(R.id.textViewRecetas);

        startForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data!=null) {
                            Receta r = new Receta();
                            r.titulo = data.getStringExtra("titulo");
                            r.categoria = data.getStringExtra("categoria");
                            r.tiempo = data.getIntExtra("tiempo", 0);
                            r.ingredientes = data.getStringExtra("ingredientes");
                            r.pasos = data.getStringExtra("pasos");

                            db.agregarReceta(r);
                            mostrarRecetas();
                        }
                    }
                }
        );

        btnAgregar.setOnClickListener(v -> {
            Receta r = new Receta();
            r.titulo = "Tortilla de patatas";
            r.categoria = "Desayuno";
            r.tiempo = 40;
            r.ingredientes = "Huevos, patatas, cebolla, aceite, sal";
            r.pasos = "1. Pelar patatas. 2. Batir huevos. 3. Freír patatas y mezclar con huevos batidos. 4. Cocinar en sartén.";
            long id = db.agregarReceta(r);
            mostrarRecetas();
        });
        mostrarRecetas();
    }
    private void mostrarRecetas() {
        List<Receta> lista = db.getRecetas();
        StringBuilder sb = new StringBuilder();
        for (Receta r: lista) {
            sb.append(r.id).append(": ").append(r.titulo)
                    .append(" (").append(r.categoria).append(")\n");
        }
        textViewRecetas.setText("Recetas:\n" + sb.toString());
    }
}