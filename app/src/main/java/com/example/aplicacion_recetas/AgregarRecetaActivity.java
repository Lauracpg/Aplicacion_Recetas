package com.example.aplicacion_recetas;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
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
        String[] categorias = {"Postre", "Cena", "Plato Principal", "Segundo Plato", "Ensalada"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        editTextTiempo = findViewById(R.id.editTextTiempo);
        editTextIngredientes = findViewById(R.id.editTextIngredientes);
        editTextIngredientes.addTextChangedListener(new TextWatcher() {
            boolean isEditing = false;
            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                int cursorPos = editTextIngredientes.getSelectionStart();
                String text = s.toString();
                StringBuilder newText = new StringBuilder();

                String[] lines = text.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty()) {
                        newText.append(line);
                    } else if (!line.startsWith("- ")) {
                        newText.append("- ").append(line);
                    } else {
                        newText.append(line);
                    }
                    if (i < lines.length - 1) newText.append("\n");
                }

                editTextIngredientes.setText(newText);
                editTextIngredientes.setSelection(Math.min(cursorPos + 2, newText.length()));
                isEditing = false;
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        editTextPasos = findViewById(R.id.editTextPasos);
        editTextPasos.addTextChangedListener(new TextWatcher() {
            boolean isEditing = false;
            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                int cursorPos = editTextPasos.getSelectionStart();
                String text = s.toString();
                StringBuilder newText = new StringBuilder();

                String[] lines = text.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty()) {
                        newText.append(line);
                    } else if (!line.matches("^\\d+\\.\\s.*")) {
                        newText.append((i + 1)).append(". ").append(line);
                    } else {
                        newText.append(line);
                    }
                    if (i < lines.length - 1) newText.append("\n");
                }

                editTextPasos.setText(newText);
                editTextPasos.setSelection(Math.min(cursorPos + 3, newText.length()));
                isEditing = false;
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        imageViewFoto = findViewById(R.id.imageViewFoto);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        btnAgregarFoto.setOnClickListener( v-> abrirGaleria());
        btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("titulo", editTextTitulo.getText().toString());
            data.putExtra("categoria", spinnerCategoria.getSelectedItem().toString());
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
