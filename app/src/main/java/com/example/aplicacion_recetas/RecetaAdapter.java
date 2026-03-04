package com.example.aplicacion_recetas;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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

        Context context = holder.itemView.getContext();
        holder.categoria.setText(context.getString(R.string.categoria) + ": " + r.categoria);
        holder.tiempo.setText(context.getString(R.string.tiempo) + ": " + r.tiempo + " "
                + context.getString(R.string.minutos));

        holder.imgFav.setImageResource(
                r.favorita ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );

        holder.imgFav.setOnClickListener( v -> {
            r.favorita = !r.favorita;
            holder.imgFav.setImageResource(
                    r.favorita ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
            );
            DBHelper db = new DBHelper(context);
            ContentValues values = new ContentValues();
            values.put(DBHelper.COLUMN_FAVORITA, r.favorita ? 1 : 0);
            SQLiteDatabase database = db.getWritableDatabase();
            database.update(DBHelper.TABLE_RECETAS, values, DBHelper.COLUMN_ID + "=?", new String[]{String.valueOf(r.id)});
            database.close();
        });

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
