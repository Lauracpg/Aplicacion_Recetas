package com.example.aplicacion_recetas;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class DetalleRecetaActivity extends AppCompatActivity implements DetalleRecetaFragment.Listener{
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

        // coge la receta enviada desde la actividad anterior
        Receta receta = (Receta) getIntent().getSerializableExtra("receta");
        // obtiene el fragment de detalle de la receta
        DetalleRecetaFragment fragment = (DetalleRecetaFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);
        // si existe y se ha recibido, muestra los datos
        if(fragment != null && receta != null) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("receta", receta);
            fragment.setArguments(bundle);
            // llama al métood del fragment para cargar la info
            fragment.mostrarReceta(receta);
        }
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

        // lanza una notificación de que se ha eliminado
        lanzarNotificacionEliminada(receta.titulo);
        // pequeño mensaje en pantalla
        Toast.makeText(this,
                getString(R.string.receta_eliminada),
                Toast.LENGTH_SHORT).show();

        // cierra la pantalla actual y vuelve atrás
        finish();
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
