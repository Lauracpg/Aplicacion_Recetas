package com.example.aplicacion_recetas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class DetalleRecetaFragment extends Fragment {
    private TextView textViewTitulo, textViewCategoria, textViewTiempo, textViewIngredientes, textViewPasos;
    private Receta recetaActual;
    private static final int REQUEST_GALERIA = 200;
    private ImageView imageViewFoto;
    private ImageView imageFavDetalle;
    private Listener listener;
    public interface Listener {
        void onEliminarDesdeDetalle(Receta receta);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirmar_eliminacion))
                    .setMessage(getString(R.string.mensaje_confirmar_eliminacion))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        if (listener != null && recetaActual != null) {
                            listener.onEliminarDesdeDetalle(recetaActual);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });
        Button btnAgregarFoto = view.findViewById(R.id.btnAgregarFoto);
        btnAgregarFoto.setOnClickListener(v -> abrirGaleria());

        ImageButton btnVolver = view.findViewById(R.id.btnVolverInicio);
        Fragment listaFragment = requireActivity()
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_lista_recetas);
        if(listaFragment != null) {
            btnVolver.setVisibility(View.GONE);
        } else {
            btnVolver.setVisibility(View.VISIBLE);
            btnVolver.setOnClickListener(v -> requireActivity().finish());
        }

        btnVolver.setOnClickListener(v -> requireActivity().finish());

        imageFavDetalle = view.findViewById(R.id.imageFavoritoDetalle);
        imageFavDetalle.setOnClickListener(v -> {
            if(recetaActual != null) {
                recetaActual.favorita = !recetaActual.favorita;
                imageFavDetalle.setImageResource(
                        recetaActual.favorita ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
                );
                DBHelper db = new DBHelper(requireContext());
                ContentValues values = new ContentValues();
                values.put(DBHelper.COLUMN_FAVORITA, recetaActual.favorita ? 1 : 0);
                SQLiteDatabase database = db.getWritableDatabase();
                database.update(DBHelper.TABLE_RECETAS, values, DBHelper.COLUMN_ID + "=?", new String[]{String.valueOf(recetaActual.id)});
                database.close();
            }
        });
        return view;
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_GALERIA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GALERIA && resultCode == Activity.RESULT_OK && data != null) {
            Uri imgSelected = data.getData();
            if(imgSelected != null && recetaActual != null) {
                requireContext().getContentResolver().takePersistableUriPermission(
                        imgSelected, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                imageViewFoto.setImageURI(imgSelected);
                imageViewFoto.setVisibility(View.VISIBLE);
                recetaActual.fotoUri = imgSelected.toString();

                DBHelper db = new DBHelper(requireContext());
                SQLiteDatabase database = db.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(DBHelper.COLUMN_FOTO, recetaActual.fotoUri);

                database.update(DBHelper.TABLE_RECETAS, values,DBHelper.COLUMN_ID + "=?",
                        new String[]{String.valueOf(recetaActual.id)});

                database.close();
            }
        }
    }

    public void mostrarReceta(Receta receta) {
        if (receta == null) return;
        recetaActual = receta;
        textViewTitulo.setText(receta.titulo);
        textViewCategoria.setText(getString(R.string.hint_categoria) + ": " + receta.categoria);
        textViewTiempo.setText(getString(R.string.hint_tiempo) + ": " + receta.tiempo + " " + getString(R.string.minutos));
        textViewIngredientes.setText(getString(R.string.hint_ingredientes) + ": " + receta.ingredientes);
        textViewPasos.setText(getString(R.string.hint_pasos) + ": " + receta.pasos);
        if(receta.fotoUri != null && !receta.fotoUri.isEmpty()) {
            imageViewFoto.setVisibility(View.VISIBLE);
            imageViewFoto.setImageURI(Uri.parse(receta.fotoUri));
        } else {
            imageViewFoto.setImageResource(android.R.color.transparent);
        }
        imageFavDetalle.setImageResource(
                receta.favorita ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
    }
}
