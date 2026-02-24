package com.example.aplicacion_recetas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DialogConfirmacion extends DialogFragment {
    public interface Listener {
        void onConfirmar();
        void onCancelar();
    }
    Listener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        listener = (Listener) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Salir")
                .setMessage("¿Seguro que quiere salir de la aplicación?")
                .setPositiveButton("Sí", (dialog, which) -> listener.onConfirmar())
                .setNegativeButton("No", (dialog, which) -> listener.onCancelar())
                .setCancelable(false);
        return builder.create();
    }
}
