package com.example.aplicacion_recetas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.List;

public class RecetaAdapter extends RecyclerView.Adapter<RecetaVH> {
    private List<Receta> listaRecetas; // lista que se mostrará en el RecylcerView
    private OnRecetaClickListener listener;

    // interfaz que define las acciones que se pueden hacer sobre una receta
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
        // se infla el layout que representa cada elemento de la lista
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receta, parent, false);

        // se crea y devuelve el ViewHolder que gestionará la vista
        return new RecetaVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecetaVH holder, int position) {
        Receta r = listaRecetas.get(position);
        holder.titulo.setText(r.titulo);

        // se obtiene el contexto para acceder a recursos (strings)
        Context context = holder.itemView.getContext();
        holder.categoria.setText(context.getString(R.string.categoria) + ": " + r.categoria);
        holder.tiempo.setText(context.getString(R.string.tiempo) + ": " + r.tiempo + " " + context.getString(R.string.minutos));
        // se establece la imagen de fav dependiendo de si está marcada o no
        holder.imgFav.setImageResource(
                r.favorita ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );

        // evento del icono de favorito
        holder.imgFav.setOnClickListener( v -> {
            r.favorita = !r.favorita; // cambia
            // se actualiza el icono
            holder.imgFav.setImageResource(
                    r.favorita
                            ? android.R.drawable.btn_star_big_on
                            : android.R.drawable.btn_star_big_off
            );

            GestorSesionUsuario sesion = new GestorSesionUsuario(context);
            Data input = new Data.Builder()
                    .putString("accion", "favorito")
                    .putInt("id", r.id)
                    .putInt("idUsuario", sesion.getUserId())
                    .putInt("favorita", r.favorita ? 1 : 0)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecetasWorker.class)
                    .setInputData(input)
                    .build();

            WorkManager.getInstance(context).enqueue(request);
        });

        // evento al pulsar elemento de la lista
        holder.itemView.setOnClickListener(v -> {
            if(listener != null) listener.onRecetaClick(r);
        });

        // evento para botón eliminar receta
        holder.btnEliminar.setOnClickListener(v -> {
            if(listener != null) listener.onRecetaEliminar(r);
        });
    }

    @Override
    public int getItemCount() {
        // devuelve el número total de recetas en la lista
        return listaRecetas.size();
    }

    // Permite actualizar la lista de recetas del adapter
    public void setRecetas(List<Receta> lista) {
        this.listaRecetas = lista;
        // notifica al RecyclerView que los datos han cambiado
        notifyDataSetChanged();
    }
}
