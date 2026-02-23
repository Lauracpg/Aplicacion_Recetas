package com.example.aplicacion_recetas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecetaAdapter extends RecyclerView.Adapter<RecetaVH> {
    private List<Receta> listaRecetas;

    public RecetaAdapter(List<Receta> lista) {
        this.listaRecetas = lista;
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
    }

    @Override
    public int getItemCount() {
        return listaRecetas.size();
    }
}
