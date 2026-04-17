package com.example.aplicacion_recetas;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AgregarRecetaActivity extends AppCompatActivity {
    EditText editTextTitulo, editTextTiempo, editTextIngredientes, editTextPasos;
    Spinner spinnerCategoria;
    ImageView imageViewFoto;
    Button btnAgregarFoto, btnGuardar;
    // código de solicitud para seleccionar imágenes desde galería
    private static final int REQUEST_GALERIA = 100;
    // URI imagen seleccionada
    private Uri fotoTemporalUri = null;
    private String imagenId;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_receta);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.receta_nueva);
        }

        // color de elementos de la barra superior e inferior del dispositivo
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        editTextTitulo = findViewById(R.id.editTextTitulo);
        editTextTiempo = findViewById(R.id.editTextTiempo);
        editTextIngredientes = findViewById(R.id.editTextIngredientes);
        editTextPasos = findViewById(R.id.editTextPasos);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        imageViewFoto = findViewById(R.id.imageViewFoto);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);

        // config del spinner de categorías con array de strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.categorias,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        // ingredientes se inicializa con "- " para formato lista
        editTextIngredientes.setText("- ");
        // TextWatcher: añade "- " cada que vez que se crea una nueva línea
        editTextIngredientes.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count == 1 && s.charAt(start) == '\n') {
                    int cursor = editTextIngredientes.getSelectionStart();
                    editTextIngredientes.getText().insert(cursor, "- ");
                }
            }
        });

        editTextPasos.setText("1. ");
        // TextWatcher: genera la numeración de los pasos
        editTextPasos.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // cuando se pulsa enter, se calcula el siguiente número de paso
                if(count == 1 && s.charAt(start) == '\n') {
                    String[] lines = s.toString().split("\n");
                    int stepCount = 0;
                    // cuenta las líneas con "num. texto"
                    for(String line: lines) {
                        if(line.matches("^\\d+\\.\\s.*")) {
                            stepCount++;
                        }
                    }
                    int nextNumber = stepCount + 1;
                    int cursor = editTextPasos.getSelectionStart();
                    editTextPasos.getText().insert(cursor, nextNumber + ". ");
                }
            }
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) return;

                    Intent data = result.getData();
                    if (data == null) return;

                    Bundle extras = data.getExtras();
                    if (extras == null) return;

                    Bitmap bitmap = (Bitmap) extras.get("data");
                    if (bitmap == null) return;

                    imageViewFoto.setImageBitmap(bitmap);
                    imageViewFoto.setVisibility(View.VISIBLE);

                    try {
                        File file = new File(getCacheDir(),
                                "receta_" + System.currentTimeMillis() + ".jpg");

                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();

                        fotoTemporalUri = Uri.fromFile(file);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirCamara();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // abre la galería del dispositivo para elegir una imagen
        btnAgregarFoto.setOnClickListener( v-> mostrarOpcionesFoto());

        // validar los datos y devolveros a la actividad que le llama
        btnGuardar.setOnClickListener(v -> guardarReceta());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (fotoTemporalUri != null) {
            outState.putString("fotoUri", fotoTemporalUri.toString());
        }
    }

    private void mostrarOpcionesFoto() {
        String[] opciones = {
                getString(R.string.galeria),
                getString(R.string.camara)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.seleccionar_opcion))
                .setItems(opciones, (dialog, which) -> {
                    if(which == 0) {
                        abrirGaleria();
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            abrirCamara();
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    }
                }).show();
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Recoge los datos introducidos, valida campos, formatea información
    // y devuelve la receta a la actividad principal
    private void guardarReceta() {
        String titulo = editTextTitulo.getText().toString().trim();
        String tiempoString = editTextTiempo.getText().toString().trim();
        String ingredientes = editTextIngredientes.getText().toString().trim();
        String pasos = editTextPasos.getText().toString().trim();
        imagenId = "receta_" + System.currentTimeMillis();

        boolean valido = true;
        // campos obligatorios
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
        // pone en mayúscula la primera letra del título
        if(titulo.length() > 0) {
            titulo = titulo.substring(0,1).toUpperCase() + titulo.substring(1);
        }

        // asegura que la primera letra de cada paso sea mayúscula
        StringBuilder pasosBuilder = new StringBuilder();
        String[] pasosLines = pasos.split("\n");

        for(int i = 0; i < pasosLines.length; i++) {
            String line = pasosLines[i];
            if(line.matches("^\\d+\\.\\s.*")) {
                int punto = line.indexOf(". ");
                if(punto + 2 < line.length()){
                    line = line.substring(0, punto + 2) +
                            Character.toUpperCase(line.charAt(punto + 2)) +
                            line.substring(punto + 3);
                }
            }
            pasosBuilder.append(line);
            if(i < pasosLines.length - 1) {
                pasosBuilder.append("\n");
            }
        }
        pasos = pasosBuilder.toString();

        // convertir el tiempo a integer
        int tiempo = Integer.parseInt(tiempoString);

        // Intent que devolverá los datos a MainActvity
        Intent data = new Intent();
        data.putExtra("titulo", titulo);
        data.putExtra("categoria", spinnerCategoria.getSelectedItem().toString());
        data.putExtra("tiempo", tiempo);
        data.putExtra("ingredientes", ingredientes);
        data.putExtra("pasos", pasos);

        if (fotoTemporalUri != null) {
            subirImagen(data);
        } else {
            data.putExtra("fotoUri", ""); // sin imagen
            setResult(RESULT_OK, data);
            actualizarWidgetDatos();
            finish();
        }
    }

    private void actualizarWidgetDatos() {
        Data input = new Data.Builder()
                .putString("accion", "obtener")
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecetasWorker.class)
                .setInputData(input)
                .build();

        WorkManager.getInstance(this).enqueue(request);
    }

    private void subirImagen(Intent data) {
        GestorSesionUsuario sesion = new GestorSesionUsuario(this);

        File file = new File(getCacheDir(), "upload.jpg");
        try (InputStream is = getContentResolver().openInputStream(fotoTemporalUri);
             FileOutputStream fos = new FileOutputStream(file)) {

            byte[] buffer = new byte[1024];
            int len;

            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Data input = new Data.Builder()
                .putString("tipo", "receta")
                .putString("ruta_local", file.getAbsolutePath())
                .putInt("idUsuario", sesion.getUserId())
                .putString("imagenId", imagenId)
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(SubirImagenWorker.class)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(this).enqueue(request);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo == null) return;
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        String rutaServidor =
                                workInfo.getOutputData().getString("ruta");

                        data.putExtra("fotoUri", rutaServidor);
                        actualizarWidgetDatos();
                        setResult(RESULT_OK, data);
                        finish();
                    }

                    if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(this,
                                "Error subiendo imagen",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Abre el selector de documentos del sistema para elegir una imagen
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_GALERIA);
    }

    // Recibe el resultado del selector de imágenes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;
        if(requestCode == REQUEST_GALERIA  && data != null) {
            Uri img = data.getData();
            if(img != null) {
                // solicita permiso persistente para poder acceder la imagen
                getContentResolver().takePersistableUriPermission(
                        img,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // mostrar la imagen seleccionada
                imageViewFoto.setImageURI(img);
                imageViewFoto.setVisibility(View.VISIBLE);
                // guarda la URI para enviarla más tarde
                fotoTemporalUri = img;
            }
        }
    }
}