package com.example.aplicacion_recetas;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements DialogConfirmacion.Listener, ListaRecetasFragment.Listener, DetalleRecetaFragment.Listener{
    DBHelper db;
    FloatingActionButton btnAgregar;

    private ActivityResultLauncher<Intent> startForResult;
    private String ultimaRecetaAgregada;
    private Receta recetaEliminar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        DrawerLayout dl = findViewById(R.id.drawer_layout);
        NavigationView nv = findViewById(R.id.navigation_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, dl, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        dl.addDrawerListener(toggle);
        toggle.syncState();

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
                        r.fotoUri = data.getStringExtra("fotoUri");
                        db.agregarReceta(r);

                        ListaRecetasFragment listaRecetasFragment = (ListaRecetasFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);
                        if (listaRecetasFragment != null) listaRecetasFragment.refreshLista();

                        ultimaRecetaAgregada = r.titulo;
                        lanzarNotificacion(ultimaRecetaAgregada);
                    }
                }
        );

        nv.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            ListaRecetasFragment listaFragment =
                    (ListaRecetasFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);
            if (id == R.id.menu_agregar_receta) {
                Intent intent = new Intent(MainActivity.this, AgregarRecetaActivity.class);
                startForResult.launch(intent);
            } else if (id == R.id.menu_ver_recetas) { // todas
                if (listaFragment != null) {
                    listaFragment.mostrarRecetas();
                }
            } else if (id == R.id.menu_ver_recetas_favoritas) {
                if (listaFragment != null) {
                    listaFragment.mostrarFavoritas();
                }
            }
            dl.closeDrawer(GravityCompat.START);
            return true;
        });

        db = new DBHelper(this);
        btnAgregar = findViewById(R.id.btnAgregarReceta);
        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AgregarRecetaActivity.class);
            startForResult.launch(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DrawerLayout dl = findViewById(R.id.drawer_layout);
                if(dl.isDrawerOpen(GravityCompat.START)) {
                    dl.closeDrawer(GravityCompat.START);
                } else {
                    new DialogConfirmacion().show(getSupportFragmentManager(), "salir");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_idioma, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_es) {
            cambiarIdioma("es");
            return true;
        } else if (id == R.id.menu_eu) {
            cambiarIdioma("eu");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void guardarIdioma(String codeIdioma) {
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        prefs.edit().putString("lang", codeIdioma).apply();
    }
    private void cambiarIdioma(String codeIdioma) {
        guardarIdioma(codeIdioma);
        finish();
        startActivity(getIntent());
    }

    @Override
    protected  void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("config", MODE_PRIVATE);
        String lang = prefs.getString("lang", Locale.getDefault().getLanguage());

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        Context context = newBase.createConfigurationContext(config);

        super.attachBaseContext(context);
    }
    @Override
    public void onRecetaSeleccionada(Receta receta) {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // actualizar detalle en fragment
            DetalleRecetaFragment detalle = (DetalleRecetaFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);
            if (detalle != null) {
                detalle.mostrarReceta(receta);
            }
        } else {
            // abrir actividad detalle
            Intent i = new Intent(this, DetalleRecetaActivity.class);
            i.putExtra("receta", receta);
            startActivity(i);
        }
    }
    @Override
    public void onRecetaEliminar(Receta receta) {
        recetaEliminar = receta;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirmar_eliminacion))
                .setMessage(getString(R.string.mensaje_confirmar_eliminacion))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> eliminarRecetaConfirmada())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void eliminarRecetaConfirmada() {
        if (recetaEliminar != null) {
            db.eliminarReceta(recetaEliminar.id);
            ListaRecetasFragment lista = (ListaRecetasFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);

            if (lista != null) {
                lista.refreshLista();
            }

            lanzarNotificacionEliminada(recetaEliminar.titulo);
            Toast.makeText(this, getString(R.string.receta_eliminada), Toast.LENGTH_SHORT).show();
            recetaEliminar = null;
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
                        .setContentTitle(getString(R.string.noti_receta_agregada_titulo))
                        .setContentText(getString(R.string.noti_receta_agregada_texto, tituloReceta))
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
                Toast.makeText(this, getString(R.string.no_notificaciones), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onEliminarDesdeDetalle(Receta receta) {
        db.eliminarReceta(receta.id);

        ListaRecetasFragment lista = (ListaRecetasFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);

        if(lista != null) {
            lista.refreshLista();
        }

        lanzarNotificacionEliminada(receta.titulo);

        Toast.makeText(this, getString(R.string.receta_eliminada), Toast.LENGTH_SHORT).show();

        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            finish();
        }
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