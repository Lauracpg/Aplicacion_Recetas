package com.example.aplicacion_recetas;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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

import org.json.JSONArray;
import org.json.JSONObject;

public class DetalleRecetaActivity extends AppCompatActivity implements DetalleRecetaFragment.Listener{
    private Receta receta;

    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate(savedInstanceState);

        // coge la receta enviada desde la actividad anterior (rotación u otra actividad)
        if (savedInstanceState != null) {
            receta = (Receta) savedInstanceState.getSerializable("receta");
        } else {
            receta = (Receta) getIntent().getSerializableExtra("receta");
        }

        // si está en horizontal, no se usa esta activity y se redirige a MainActivity para mostrar detalle fragment
        if(getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("recetaActual", receta); // pasar receta actual
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

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

        // config volver en la ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.detalle_receta);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        int recetaIdIntent = getIntent().getIntExtra("receta_id", -1);

        mostrarEnFragment(receta);

        // si viene un id, se carga la receta desde el servidor
        if (recetaIdIntent != -1) {
            cargarRecetaDesdeId(recetaIdIntent);
        } else {
            receta = (Receta) getIntent().getSerializableExtra("receta");

            if (receta != null) {
                mostrarEnFragment(receta);
            }
        }
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

    // mostrar receta dentro del fragment de detalle
    private void mostrarEnFragment(Receta receta) {
        // obtiene el fragment de detalle de la receta
        DetalleRecetaFragment fragment =
                (DetalleRecetaFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_detalle_receta);
        // si existe y se ha recibido, muestra los datos
        if (fragment != null) {
            fragment.setReceta(receta);
        }
    }

    // cargar una receta desde el servidor filtrando por id
    private void cargarRecetaDesdeId(int id) {
        GestorSesionUsuario sesion = new GestorSesionUsuario(this);
        Data input = new Data.Builder()
                .putString("accion", "obtener")
                .putInt("idUsuario", sesion.getUserId())
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(RecetasWorker.class)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(this).enqueue(request);

        // observar la respuesta del servidor y buscar la receta por id
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {

                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String json = workInfo.getOutputData().getString("response");

                        try {
                            JSONObject obj = new JSONObject(json);
                            JSONArray recetas = obj.getJSONArray("recetas");

                            for (int i = 0; i < recetas.length(); i++) {
                                JSONObject r = recetas.getJSONObject(i);

                                if (r.getInt("id") == id) {
                                    Receta receta = new Receta();
                                    receta.id = id;
                                    receta.titulo = r.getString("titulo");
                                    receta.categoria = r.getString("categoria");
                                    receta.tiempo = r.getInt("tiempo");
                                    receta.ingredientes = r.getString("ingredientes");
                                    receta.pasos = r.getString("pasos");
                                    receta.fotoUri = r.optString("fotoUri", null);
                                    receta.favorita = r.optInt("favorita", 0) == 1;

                                    this.receta = receta;
                                    mostrarEnFragment(receta);
                                    break;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // eliminar receta desde el fragment de detalle
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
                                refrescarWidget();
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("recetaEliminada", true);
                                setResult(RESULT_OK, resultIntent);
                                // cierra la pantalla actual y vuelve atrás
                                finish();
                            }
                        });
    }

    // notifica a MainActivity de que hay cambios en favoritos
    @Override
    public void onFavoritoCambiado(Receta recetaActual) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("reload", true);
        setResult(RESULT_OK, resultIntent);
    }

    // fuerza actualización del widget tras cambios en recetas
    private void refrescarWidget() {
        GestorSesionUsuario sesion = new GestorSesionUsuario(this);
        Data input = new Data.Builder()
                .putString("accion", "obtener")
                .putInt("idUsuario", sesion.getUserId())
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(RecetasWorker.class)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(this).enqueue(request);
    }

    // Crea una notificación indicando que se ha eliminado una receta
    private void lanzarNotificacionEliminada(String titulo) {
        String canalId = "canal_recetas";
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // construye la notificación
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
