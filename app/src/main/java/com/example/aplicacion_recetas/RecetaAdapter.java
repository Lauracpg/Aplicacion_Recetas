package com.example.aplicacion_recetas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecetaAdapter extends RecyclerView.Adapter<RecetaVH> {
    private List<Receta> listaRecetas;
    private OnRecetaClickListener listener;

    public interface OnRecetaClickListener {
        void onRecetaClick(Receta receta);
        void onRecetaEliminar(Receta receta);
    }

    public RecetaAdapter(List<Receta> lista, OnRecetaClickListener listener) {
        this.listaRecetas = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecetaVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receta, parent, false);
        return new RecetaVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecetaVH holder, int position) {
        Receta r = listaRecetas.get(position);
        holder.titulo.setText(r.titulo);
        holder.categoria.setText("Categoría: " + r.categoria);
        holder.tiempo.setText("Tiempo: " + r.tiempo + " min");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRecetaClick(r);
        });

        // botón eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.onRecetaEliminar(r);
        });
    }

    @Override
    public int getItemCount() {
        return listaRecetas.size();
    }

    public void setRecetas(List<Receta> lista) {
        this.listaRecetas = lista;
    }
}
