package com.example.aplicacion_recetas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DetalleRecetaFragment extends Fragment {
    private TextView textViewTitulo, textViewCategoria, textViewTiempo, textViewIngredientes, textViewPasos;
    private ImageView imageViewFoto;
    private ImageView imageFavDetalle;
    private Receta recetaActual;
    private static final int REQUEST_GALERIA = 200;
    private Listener listener;
    public interface Listener { // interfaz para fragment -> activity
        void onEliminarDesdeDetalle(Receta receta);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // coge la receta enviada en el bundle
        if(getArguments() != null) {
            recetaActual = (Receta) getArguments().getSerializable("receta");
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
        Button btnAgregarFoto = view.findViewById(R.id.btnAgregarFoto);
        btnAgregarFoto.setOnClickListener(v -> abrirGaleria());

        ImageButton btnVolver = view.findViewById(R.id.btnVolverInicio);

        Fragment listaFragment = requireActivity()
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_lista_recetas);

        // si está en landscape oculta el botón de volver
        if(listaFragment != null) {
            btnVolver.setVisibility(View.GONE);
        } else {
            btnVolver.setVisibility(View.VISIBLE);
            btnVolver.setOnClickListener(v -> requireActivity().finish());
        }

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
                        .putInt("favorita", recetaActual.favorita ? 1 : 0)
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
                            }
                        });
            }
        });
        return view;
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
                recetaActual.fotoUri = imgSelected.toString();

                try {
                    InputStream is = requireContext().getContentResolver().openInputStream(imgSelected);
                    File file = new File(requireContext().getCacheDir(), "receta_" + recetaActual.id + ".jpg");
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    fos.close();
                    is.close();

                    GestorSesionUsuario sesion = new GestorSesionUsuario(requireContext());
                    Data input = new Data.Builder()
                            .putString("tipo", "receta")
                            .putInt("id", recetaActual.id)
                            .putInt("idUsuario", sesion.getUserId())
                            .putString("ruta_local", file.getAbsolutePath())
                            .build();

                    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SubirImagenWorker.class)
                            .setInputData(input)
                            .build();

                    WorkManager.getInstance(requireContext()).enqueue(request);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Cargar receta en la interfaz
    public void mostrarReceta(Receta receta) {
        if (receta == null) return;
        recetaActual = receta;
        textViewTitulo.setText(receta.titulo);
        textViewCategoria.setText(receta.categoria);
        textViewTiempo.setText(receta.tiempo + " " + getString(R.string.minutos));
        textViewIngredientes.setText(receta.ingredientes);
        textViewPasos.setText(receta.pasos);

        // mostrar foto si tiene
        if(receta.fotoUri != null && !receta.fotoUri.isEmpty()) {
            imageViewFoto.setVisibility(View.VISIBLE);
            imageViewFoto.setImageURI(Uri.parse(receta.fotoUri));
        } else {
            imageViewFoto.setImageResource(android.R.color.transparent);
        }

        // icono favorito
        imageFavDetalle.setImageResource(
                receta.favorita
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off
        );
    }
}
