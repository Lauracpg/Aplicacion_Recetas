package com.example.aplicacion_recetas;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AgregarRecetaActivity extends AppCompatActivity {
    EditText editTextTitulo, editTextCategoria, editTextTiempo, editTextIngredientes, editTextPasos;
    ImageView imageViewFoto;
    Button btnAgregarFoto;
    private static final int REQUEST_GALERIA = 100;
    private String fotoUri = null;
    Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_receta);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        editTextTitulo = findViewById(R.id.editTextTitulo);
        editTextCategoria = findViewById(R.id.editTextCategoria);
        editTextTiempo = findViewById(R.id.editTextTiempo);
        editTextIngredientes = findViewById(R.id.editTextIngredientes);
        editTextPasos = findViewById(R.id.editTextPasos);
        imageViewFoto = findViewById(R.id.imageViewFoto);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        btnAgregarFoto.setOnClickListener( v-> abrirGaleria());
        btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("titulo", editTextTitulo.getText().toString());
            data.putExtra("categoria", editTextCategoria.getText().toString());
            data.putExtra("tiempo", Integer.parseInt(editTextTiempo.getText().toString()));
            data.putExtra("ingredientes", editTextIngredientes.getText().toString());
            data.putExtra("pasos", editTextPasos.getText().toString());
            data.putExtra("fotoUri", fotoUri);

            setResult(RESULT_OK, data);
            finish();
        });
        ImageButton btnVolver = findViewById(R.id.btnVolverInicio);
        btnVolver.setOnClickListener(v -> finish());
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_GALERIA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_GALERIA && resultCode == RESULT_OK && data != null) {
            Uri imgSelected = data.getData();
            if(imgSelected != null) {
                getContentResolver().takePersistableUriPermission(imgSelected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                imageViewFoto.setImageURI(imgSelected);
                imageViewFoto.setVisibility(View.VISIBLE);
                fotoUri = imgSelected.toString();
            }
        }
    }
}
