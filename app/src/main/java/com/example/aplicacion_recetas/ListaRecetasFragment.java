package com.example.aplicacion_recetas;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ListaRecetasFragment extends Fragment {
    public interface Listener {
        void onRecetaSeleccionada(Receta receta);
        void onRecetaEliminar(Receta receta);
    }

    private Listener listener;
    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private DBHelper db;
    private boolean mostrarFavoritas = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new ClassCastException(context.toString() + " debe implementar ListaRecetasFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_recetas, container, false);
        recyclerView = view.findViewById(R.id.recyclerRecetas);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        db = new DBHelper(view.getContext());

        adapter = new RecetaAdapter(new ArrayList<>(), new RecetaAdapter.OnRecetaClickListener() {
            @Override
            public void onRecetaClick(Receta receta) {
                if (listener != null) listener.onRecetaSeleccionada(receta);
            }

            @Override
            public void onRecetaEliminar(Receta receta) {
                if(listener != null) listener.onRecetaEliminar(receta);
            }
        });
        recyclerView.setAdapter(adapter);
        refreshLista();
        return view;
    }
    public void mostrarRecetas() {
        mostrarFavoritas = false;
        List<Receta> todas = db.getRecetas();
        adapter.setRecetas(todas);
        adapter.notifyDataSetChanged();
    }

    public void mostrarFavoritas() {
        mostrarFavoritas = true;
        List<Receta> favoritas = db.getRecetasFavoritas();
        adapter.setRecetas(favoritas);
        adapter.notifyDataSetChanged();
    }

    public void refreshLista() {
        if (adapter != null) {
            if (mostrarFavoritas) {
                adapter.setRecetas(db.getRecetasFavoritas());
            } else {
                adapter.setRecetas(db.getRecetas());
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLista();
    }
}
