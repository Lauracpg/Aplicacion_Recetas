package com.example.aplicacion_recetas;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AgregarRecetaActivity extends AppCompatActivity {
    EditText editTextTitulo, editTextTiempo, editTextIngredientes, editTextPasos;
    Spinner spinnerCategoria;
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

        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.categorias,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        editTextTiempo = findViewById(R.id.editTextTiempo);

        editTextIngredientes = findViewById(R.id.editTextIngredientes);
        editTextIngredientes.setText("- ");
        editTextIngredientes.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && s.charAt(start) == '\n') {
                    EditText et = editTextIngredientes;
                    int cursor = et.getSelectionStart();
                    et.getText().insert(cursor, "- ");
                }
            }
        });

        editTextPasos = findViewById(R.id.editTextPasos);
        editTextPasos.setText("1. ");
        editTextPasos.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && s.charAt(start) == '\n') {
                    String text = s.toString();
                    String[] lines = text.split("\n");
                    int stepCount = 0;
                    for(String line: lines) {
                        if(line.matches("^\\d+\\.\\s.*")) {
                            stepCount++;
                        }
                    }
                    int nextNumber = stepCount + 1;
                    EditText et = editTextPasos;
                    int cursor = et.getSelectionStart();
                    et.getText().insert(cursor, nextNumber + ". ");
                }
            }
        });

        imageViewFoto = findViewById(R.id.imageViewFoto);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        btnAgregarFoto.setOnClickListener( v-> abrirGaleria());
        btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> {
            String titulo = editTextTitulo.getText().toString().trim();
            String tiempoString = editTextTiempo.getText().toString().trim();
            String ingredientes = editTextIngredientes.getText().toString().trim();
            String pasos = editTextPasos.getText().toString().trim();
            boolean valido = true;

            if(titulo.isEmpty()){
                editTextTitulo.setError(getString(R.string.error_titulo));
                valido = false;
            }

            if(tiempoString.isEmpty()){
                editTextTiempo.setError(getString(R.string.error_tiempo));
                valido = false;
            }

            if(ingredientes.isEmpty()){
                editTextIngredientes.setError(getString(R.string.error_ingredientes));
                valido = false;
            }

            if(pasos.isEmpty()){
                editTextPasos.setError(getString(R.string.error_pasos));
                valido = false;
            }

            if(!valido){
                return;
            }

            if(titulo.length() > 0) {
                titulo = titulo.substring(0,1).toUpperCase() + titulo.substring(1);
            }

            String[] pasosLines = pasos.split("\n");
            StringBuilder pasosBuilder = new StringBuilder();
            for(int i = 0; i < pasosLines.length; i++) {
                String line = pasosLines[i];
                if(line.matches("^\\d+\\.\\s.*")) {
                    int punto = line.indexOf(". ");
                    if(punto + 2 < line.length()){
                        line = line.substring(0, punto + 2) + line.substring(punto + 2, punto + 3).toUpperCase() + line.substring(punto + 3);
                    }
                }
                pasosBuilder.append(line);
                if(i < pasosLines.length - 1) pasosBuilder.append("\n");
            }
            pasos = pasosBuilder.toString();

            int tiempo = Integer.parseInt(tiempoString);

            Intent data = new Intent();
            data.putExtra("titulo", titulo);
            data.putExtra("categoria", spinnerCategoria.getSelectedItem().toString());
            data.putExtra("tiempo", tiempo);
            data.putExtra("ingredientes", ingredientes);
            data.putExtra("pasos", pasos);
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
