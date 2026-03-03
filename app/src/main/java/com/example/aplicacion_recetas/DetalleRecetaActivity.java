package com.example.aplicacion_recetas;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class DetalleRecetaActivity extends AppCompatActivity implements DetalleRecetaFragment.Listener{
    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_receta);

        Receta receta = (Receta) getIntent().getSerializableExtra("receta");

        DetalleRecetaFragment fragment = (DetalleRecetaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);

        if(fragment != null && receta != null) {
            fragment.mostrarReceta(receta);
        }
    }

    @Override
    public void onEliminarDesdeDetalle(Receta receta) {
        DBHelper db = new DBHelper(this);
        db.eliminarReceta(receta.id);
        lanzarNotificacionEliminada(receta.titulo);
        Toast.makeText(this, getString(R.string.receta_eliminada), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void lanzarNotificacionEliminada(String titulo) {
        String canalId = "canal_recetas";
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, canalId)
                        .setSmallIcon(android.R.drawable.ic_delete)
                        .setContentTitle(getString(R.string.noti_receta_eliminada_titulo))
                        .setContentText(getString(R.string.noti_receta_eliminada_texto, titulo))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
