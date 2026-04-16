package com.example.aplicacion_recetas;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetalleRecetaFragment extends Fragment {
    private TextView textViewTitulo, textViewCategoria, textViewTiempo, textViewIngredientes, textViewPasos;
    private ImageView imageViewFoto;
    private ImageView imageFavDetalle;
    private Button btnAgregarFoto;
    private Receta recetaActual;
    private static final int REQUEST_GALERIA = 200;
    private Listener listener;

    private Uri imagenSeleccionada = null;
    private boolean imgChanged = false;
    private Uri fotoCamaraUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    public interface Listener { // interfaz para fragment -> activity
        void onEliminarDesdeDetalle(Receta receta);

        void onFavoritoCambiado(Receta recetaActual);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // comprueba si la activity implementa el listener
        if (context instanceof Listener) {
            listener = (Listener) context;
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (recetaActual != null) {
            mostrarReceta(recetaActual);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_detalle_receta, container, false);

        textViewTitulo = view.findViewById(R.id.textViewTitulo);
        textViewCategoria = view.findViewById(R.id.textViewCategoria);
        textViewTiempo = view.findViewById(R.id.textViewTiempo);
        textViewIngredientes = view.findViewById(R.id.textViewIngredientes);
        textViewPasos = view.findViewById(R.id.textViewPasos);
        imageViewFoto = view.findViewById(R.id.imageViewFoto);

        Button btnEliminar = view.findViewById(R.id.btnEliminarDetalle);
        btnEliminar.setOnClickListener(v -> {
            // diálogo de confirmación antes de eliminar
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirmar_eliminacion))
                    .setMessage(getString(R.string.mensaje_confirmar_eliminacion))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        if(listener != null && recetaActual != null) {
                            listener.onEliminarDesdeDetalle(recetaActual);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });

        // añadir o cambiar foto
        btnAgregarFoto = view.findViewById(R.id.btnAgregarFoto);
        btnAgregarFoto.setOnClickListener(v -> mostrarOpcionesFoto());

        Button btnGuardar = view.findViewById(R.id.btnGuardarCambios);
        btnGuardar.setOnClickListener(v -> guardarCambios());

        Fragment listaFragment = requireActivity()
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_lista_recetas);

        imageFavDetalle = view.findViewById(R.id.imageFavoritoDetalle);
        imageFavDetalle.setOnClickListener(v -> {
            if(recetaActual != null) {
                if (recetaActual == null) return;
                boolean nuevoEstado = !recetaActual.favorita;

                GestorSesionUsuario sesion = new GestorSesionUsuario(requireContext());
                Data input = new Data.Builder()
                        .putString("accion", "favorito")
                        .putInt("id", recetaActual.id)
                        .putInt("idUsuario", sesion.getUserId())
                        .putInt("favorita", nuevoEstado ? 1 : 0)
                        .build();

                OneTimeWorkRequest request =
                        new OneTimeWorkRequest.Builder(RecetasWorker.class)
                                .setInputData(input)
                                .build();

                WorkManager.getInstance(requireContext()).enqueue(request);
                WorkManager.getInstance(requireContext())
                        .getWorkInfoByIdLiveData(request.getId())
                        .observe(getViewLifecycleOwner(), workInfo -> {

                            if (workInfo != null && workInfo.getState().isFinished()) {
                                recetaActual.favorita = nuevoEstado;
                                imageFavDetalle.setImageResource(
                                        nuevoEstado
                                                ? android.R.drawable.btn_star_big_on
                                                : android.R.drawable.btn_star_big_off
                                );
                                if (listener != null) {
                                    listener.onFavoritoCambiado(recetaActual);
                                }
                            }
                        });
            }
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) return;

                    Intent data = result.getData();
                    if (data == null) return;

                    Bundle extras = data.getExtras();
                    if (extras == null) return;

                    Bitmap bitmap = (Bitmap) extras.get("data");
                    if (bitmap == null) return;

                    imageViewFoto.setImageBitmap(bitmap);
                    imageViewFoto.setVisibility(View.VISIBLE);

                    try {
                        File file = new File(
                                requireContext().getCacheDir(),
                                "receta_" + System.currentTimeMillis() + ".jpg"
                        );

                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();

                        fotoCamaraUri = Uri.fromFile(file);
                        imagenSeleccionada = fotoCamaraUri;
                        recetaActual.fotoUri = fotoCamaraUri.toString();
                        imgChanged = true;

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
                        Toast.makeText(requireContext(),
                                "Permiso de cámara denegado",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        return view;
    }

    private void mostrarOpcionesFoto() {
        String[] opciones = {
                getString(R.string.galeria),
                getString(R.string.camara)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.seleccionar_opcion))
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        abrirGaleria();
                    } else {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
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

    private void guardarCambios() {
        if (recetaActual == null) return;

        GestorSesionUsuario sesion = new GestorSesionUsuario(requireContext());

        if(imagenSeleccionada != null && imgChanged) {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(imagenSeleccionada);
                File file = new File(requireContext().getCacheDir(), "receta_" + recetaActual.id + ".jpg");
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
                is.close();

                Data input = new Data.Builder()
                        .putString("tipo", "receta")
                        .putString("ruta_local", file.getAbsolutePath())
                        .putInt("idReceta", recetaActual.id)
                        .putInt("idUsuario", sesion.getUserId())
                        .build();

                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SubirImagenWorker.class)
                        .setInputData(input)
                        .build();

                WorkManager.getInstance(requireContext()).enqueue(request);

                WorkManager.getInstance(requireContext())
                        .getWorkInfoByIdLiveData(request.getId())
                        .observe(getViewLifecycleOwner(), workInfo -> {

                            if (workInfo == null) return;

                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                                String nuevaRuta = workInfo.getOutputData().getString("ruta");

                                if (nuevaRuta != null) {
                                    recetaActual.fotoUri = nuevaRuta;
                                    cargarImagenServidor(nuevaRuta);
                                }

                                imgChanged = false;
                                imagenSeleccionada = null;

                                Toast.makeText(requireContext(),
                                        getString(R.string.cambios),
                                        Toast.LENGTH_SHORT).show();
                            }

                            if (workInfo.getState() == WorkInfo.State.FAILED) {
                                Toast.makeText(requireContext(),
                                        "Error al guardar cambios",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarImagenServidor(String ruta) {
        String urlCompleta = "http://34.175.70.22:81/" + ruta;
        new Thread(() -> {
            try {
                URL url = new URL(urlCompleta);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                requireActivity().runOnUiThread(() -> {
                    imageViewFoto.setVisibility(View.VISIBLE);
                    imageViewFoto.setImageBitmap(bitmap);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Abre la galería para seleccionar una imagen
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_GALERIA);
    }

    // Recibe la imagen seleccionada
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_GALERIA && resultCode == Activity.RESULT_OK && data != null) {
            Uri imgSelected = data.getData();
            if(imgSelected != null && recetaActual != null) {
                // permiso para poder acceder a la imagen después
                requireContext().getContentResolver()
                        .takePersistableUriPermission(imgSelected,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                // mostrar imagen
                imageViewFoto.setImageURI(imgSelected);
                imageViewFoto.setVisibility(View.VISIBLE);

                imagenSeleccionada = imgSelected;
                recetaActual.fotoUri = imgSelected.toString();
                imgChanged = true;
            }
        }
    }

    public void setReceta(Receta receta) {
        this.recetaActual = receta;
        mostrarReceta(receta);
    }

    // Cargar receta en la interfaz
    public void mostrarReceta(Receta receta) {
        if (receta == null || getView() == null) return;

        recetaActual = receta;
        textViewTitulo.setText(receta.titulo);
        textViewCategoria.setText(receta.categoria);
        textViewTiempo.setText(receta.tiempo + " " + getString(R.string.minutos));
        textViewIngredientes.setText(receta.ingredientes);
        textViewPasos.setText(receta.pasos);

        // icono favorito
        if (imageFavDetalle != null) {
            imageFavDetalle.setImageResource(
                    receta.favorita
                            ? android.R.drawable.btn_star_big_on
                            : android.R.drawable.btn_star_big_off
            );
        }

        // mostrar foto si tiene
        if (receta.fotoUri != null && !receta.fotoUri.isEmpty()) {
            imageViewFoto.setVisibility(View.VISIBLE);
            cargarImagenServidor(receta.fotoUri);
            btnAgregarFoto.setText(getString(R.string.btn_cambiar_foto));
        } else {
            imageViewFoto.setVisibility(View.GONE);
            btnAgregarFoto.setText(getString(R.string.btn_agregar_foto));
        }
    }
}
