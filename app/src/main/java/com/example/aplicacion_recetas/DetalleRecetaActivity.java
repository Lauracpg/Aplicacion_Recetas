package com.example.aplicacion_recetas;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class DetalleRecetaActivity extends AppCompatActivity implements DetalleRecetaFragment.Listener{
    private Receta receta;
    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_receta);
        // color de elementos de la barra superior e inferior del dispositivo
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.detalle_receta);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // coge la receta enviada desde la actividad anterior
        if(savedInstanceState != null) {
            receta = (Receta) savedInstanceState.getSerializable("receta");
        } else {
            receta = (Receta) getIntent().getSerializableExtra("receta");
        }

        // obtiene el fragment de detalle de la receta
        DetalleRecetaFragment fragment = (DetalleRecetaFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);
        // si existe y se ha recibido, muestra los datos
        fragment.setReceta(receta);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("receta", receta);
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Cuando desde el fragment se pulsa eliminar receta
    @Override
    public void onEliminarDesdeDetalle(Receta receta) {
        GestorSesionUsuario sesion = new GestorSesionUsuario(this);
        Data input = new Data.Builder()
                .putString("accion", "eliminar")
                .putInt("id", receta.id)
                .putInt("idUsuario", sesion.getUserId())
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(RecetasWorker.class)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(this).enqueue(request);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                // lanza una notificación de que se ha eliminado
                                lanzarNotificacionEliminada(receta.titulo);
                                // pequeño mensaje en pantalla
                                Toast.makeText(this,
                                        getString(R.string.receta_eliminada),
                                        Toast.LENGTH_SHORT).show();

                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("recetaEliminada", true);
                                setResult(RESULT_OK, resultIntent);
                                // cierra la pantalla actual y vuelve atrás
                                finish();
                            }
                        });
    }

    // Crea un notificación indicando que se ha eliminado una receta
    private void lanzarNotificacionEliminada(String titulo) {
        String canalId = "canal_recetas";
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // construiye la notificación
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, canalId)
                        .setSmallIcon(android.R.drawable.ic_delete)
                        .setContentTitle(getString(R.string.noti_receta_eliminada_titulo))
                        .setContentText(getString(R.string.noti_receta_eliminada_texto, titulo))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);
        // se envía la notificación
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
