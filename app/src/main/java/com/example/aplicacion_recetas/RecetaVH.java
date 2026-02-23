package com.example.aplicacion_recetas;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecetaVH extends RecyclerView.ViewHolder {
    TextView titulo, categoria, tiempo;
    public RecetaVH(@NonNull View itemView) {
        super(itemView);
        titulo = itemView.findViewById(R.id.textTitulo);
        categoria = itemView.findViewById(R.id.textCategoria);
        tiempo = itemView.findViewById(R.id.textTiempo);
    }
}
