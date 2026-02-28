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

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);
        try {
            listener = (Listener) context;
        } catch (ClassCastException e) {
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

        mostrarRecetas();
        return view;
    }
    private void mostrarRecetas() {
        List<Receta> lista = db.getRecetas();
        adapter = new RecetaAdapter(lista, new RecetaAdapter.OnRecetaClickListener() {
            @Override
            public void onRecetaClick(Receta receta) {
                if (listener != null) listener.onRecetaSeleccionada(receta);
            }
            @Override
            public void onRecetaEliminar(Receta receta) {
                if (listener != null) listener.onRecetaEliminar(receta);
                refreshLista();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    public void refreshLista() {
        if (adapter != null) {
            adapter.setRecetas(db.getRecetas());
            adapter.notifyDataSetChanged();
        }
    }
}
