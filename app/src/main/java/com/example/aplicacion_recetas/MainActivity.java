package com.example.aplicacion_recetas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    DBHelper db;
    Button btnAgregar;
    TextView textViewRecetas;
    ActivityResultLauncher<Intent> startForResult;

    RecyclerView recyclerView;
    RecetaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        btnAgregar = findViewById(R.id.btnAgregarReceta);
        recyclerView = findViewById(R.id.recyclerRecetas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
            Intent intent = new Intent(MainActivity.this, AgregarRecetaActivity.class);
            startForResult.launch(intent);
        });
    }
    private void mostrarRecetas() {
        List<Receta> lista = db.getRecetas();
        adapter = new RecetaAdapter(lista);
        recyclerView.setAdapter(adapter);
    }
}