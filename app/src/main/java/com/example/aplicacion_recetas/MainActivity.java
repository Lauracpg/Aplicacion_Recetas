package com.example.aplicacion_recetas;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Conference;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements DialogConfirmacion.Listener, ListaRecetasFragment.Listener{
    DBHelper db;
    Button btnAgregar;

    private ActivityResultLauncher<Intent> startForResult;
    private String ultimaRecetaAgregada;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        btnAgregar = findViewById(R.id.btnAgregarReceta);

        startForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Receta r = new Receta();
                        r.titulo = data.getStringExtra("titulo");
                        r.categoria = data.getStringExtra("categoria");
                        r.tiempo = data.getIntExtra("tiempo", 0);
                        r.ingredientes = data.getStringExtra("ingredientes");
                        r.pasos = data.getStringExtra("pasos");

                        db.agregarReceta(r);

                        ListaRecetasFragment listaRecetasFragment = (ListaRecetasFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);
                        if (listaRecetasFragment != null) listaRecetasFragment.refreshLista();

                        ultimaRecetaAgregada = r.titulo;
                        lanzarNotificacion(ultimaRecetaAgregada);
                    }
                }
        );

        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AgregarRecetaActivity.class);
            startForResult.launch(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new DialogConfirmacion().show(getSupportFragmentManager(), "salir");
            }
        });
    }

    @Override
    public void onRecetaSeleccionada(Receta receta) {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // actualizar detalle en fragment
            DetalleRecetaFragment detalle = (DetalleRecetaFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);
            if (detalle != null) detalle.mostrarReceta(receta);
        } else {
            // abrir actividad detalle
            Intent i = new Intent(this, DetalleRecetaActivity.class);
            i.putExtra("receta", receta);
            startActivity(i);
        }
    }
    @Override
    public void onRecetaEliminar(Receta receta) {
        db.eliminarReceta(receta.id);
        ListaRecetasFragment listaRecetasFragment = (ListaRecetasFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);
        if(listaRecetasFragment != null) {
            listaRecetasFragment.refreshLista();
        }
    }

    @Override
    public void onConfirmar() {
        finish();
    }

    @Override
    public void onCancelar() {
        // nada
    }

    private void lanzarNotificacion(String tituloReceta) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
                return;
            }
            enviarNotificacion(tituloReceta);
        }
    }
    private void enviarNotificacion(String tituloReceta) {
        String canalId = "canal_recetas";
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalId,
                    "Canal Recetas",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            canal.setDescription("Notificaciones de recetas");
            manager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, canalId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Receta añadida")
                        .setContentText("Se ha añadido: " + tituloReceta)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 100) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ultimaRecetaAgregada != null) {
                    enviarNotificacion(ultimaRecetaAgregada);
                }
            } else {
                Toast.makeText(this, "No se podrán enviar notificaciones de nuevas recetas", Toast.LENGTH_SHORT).show();
            }
        }
    }
}