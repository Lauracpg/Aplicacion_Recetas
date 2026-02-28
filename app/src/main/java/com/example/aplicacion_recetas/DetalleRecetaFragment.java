package com.example.aplicacion_recetas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DetalleRecetaFragment extends Fragment {
    private TextView textViewTitulo, textViewCategoria, textViewTiempo, textViewIngredientes, textViewPasos;

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
        return view;
    }

    public void mostrarReceta(Receta receta) {
        textViewTitulo.setText(receta.titulo);
        textViewCategoria.setText(receta.categoria);
        textViewTiempo.setText(receta.tiempo + " min");
        textViewIngredientes.setText(receta.ingredientes);
        textViewPasos.setText(receta.pasos);
    }
}
