package com.example.aplicacion_recetas;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DetalleRecetaFragment extends Fragment {
    private TextView textViewTitulo, textViewCategoria, textViewTiempo, textViewIngredientes, textViewPasos;
    private Receta recetaActual;
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
        return view;
    }

    public void mostrarReceta(Receta receta) {
        recetaActual = receta;
        textViewTitulo.setText(receta.titulo);
        textViewCategoria.setText(receta.categoria);
        textViewTiempo.setText(receta.tiempo + " min");
        textViewIngredientes.setText(receta.ingredientes);
        textViewPasos.setText(receta.pasos);
    }
}
