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
    // interfaz para comunicar eventos del fragmento con la actividad que lo contiene
    public interface Listener {
        void onRecetaSeleccionada(Receta receta);
        void onRecetaEliminar(Receta receta);
    }

    private Listener listener;
    private RecyclerView recyclerView;
    private RecetaAdapter adapter;
    private DBHelper db;

    // indica si se están mostrando solo las recetas favoritas
    private boolean mostrarFavoritas = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // comprueba si la actividad que contiene el fragment implementa la interfaz listener
        if(context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new ClassCastException(context.toString() + " debe implementar ListaRecetasFragment.Listener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // se infla el layout del fragmento
        View view = inflater.inflate(R.layout.fragment_lista_recetas, container, false);
        recyclerView = view.findViewById(R.id.recyclerRecetas);
        // se establece un LinearLayoutManager para que aparezca en forma de lista vertical
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setHasFixedSize(true); // el tamaño del RecyclerView no cambia

        db = new DBHelper(view.getContext());
        // crea el adapter con una lista vacía al principio
        adapter = new RecetaAdapter(new ArrayList<>(), new RecetaAdapter.OnRecetaClickListener() {

            // cuando se pulsa una receta se notifica a la actividad
            @Override
            public void onRecetaClick(Receta receta) {
                if(listener != null) listener.onRecetaSeleccionada(receta);
            }

            // cuando se quiere eliminar una receta se notifica a la actividad
            @Override
            public void onRecetaEliminar(Receta receta) {
                if(listener != null) listener.onRecetaEliminar(receta);
            }
        });
        // asignar el adapter al RecyclerView
        recyclerView.setAdapter(adapter);
        // carga las recetas desde la db
        refreshLista();
        return view;
    }

    // Muestra todas las recetas almacenadas
    public void mostrarRecetas() {
        mostrarFavoritas = false;
        List<Receta> todas = db.getRecetas();
        // actualiza el adapter con la lista obtenida
        adapter.setRecetas(todas);
    }

    // Muestra solo las recetas favoritas

    public void mostrarFavoritas() {
        mostrarFavoritas = true;
        List<Receta> favoritas = db.getRecetasFavoritas();
        adapter.setRecetas(favoritas);
    }

    // Actualiza la lista mostrada en función del modo (todas o favoritas)
    public void refreshLista() {
        if(adapter != null) {
            if(mostrarFavoritas) { // solo favoritas
                adapter.setRecetas(db.getRecetasFavoritas());
            } else { // todas
                adapter.setRecetas(db.getRecetas());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // cada vez que el fragment vuelve a primer plano se refresca la lista
        refreshLista();
    }
}
