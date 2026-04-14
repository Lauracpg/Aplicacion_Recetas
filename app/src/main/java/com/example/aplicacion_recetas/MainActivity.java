package com.example.aplicacion_recetas;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements DialogConfirmacion.Listener,
        ListaRecetasFragment.Listener, DetalleRecetaFragment.Listener{
    DBHelper db;
    FloatingActionButton btnAgregar;

    private ActivityResultLauncher<Intent> startForResult;
    private Receta ultimaRecetaAgregada;
    private Receta recetaEliminar;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ImageView imgPerfil;
    private Bitmap fotoPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);

        // color de elementos de la barra inferior y superior del dispositivo
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        // config de la toolbar como ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        // config del Navigation Drawer
        DrawerLayout dl = findViewById(R.id.drawer_layout);
        NavigationView nv = findViewById(R.id.navigation_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, dl, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        dl.addDrawerListener(toggle);
        toggle.syncState();

        View headerView = nv.getHeaderView(0);
        TextView txtNombre = headerView.findViewById(R.id.txtNombreUsuario);
        TextView txtEmail = headerView.findViewById(R.id.txtEmailUsuario);
        imgPerfil = headerView.findViewById(R.id.imgPerfil);

        GestorSesionUsuario sesion = new GestorSesionUsuario(this);
        txtNombre.setText(sesion.getUserName());
        txtEmail.setText(sesion.getUserEmail());

        String rutaFoto = sesion.getFoto();

        if (rutaFoto != null) {
            String urlCompleta = "http://34.175.70.22:81/" + rutaFoto;
            new Thread(() -> {
                try {
                    URL url = new URL(urlCompleta);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);

                    runOnUiThread(() -> imgPerfil.setImageBitmap(bitmap));

                } catch (Exception e) {
                    //e.printStackTrace();
                    Log.e("IMG", "Error cargando imagen", e);

                }
            }).start();
        }

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();

                        if (extras != null) {
                            Object data = extras.get("data");

                            if (data instanceof Bitmap) {
                                Bitmap bitmap = (Bitmap) data;

                                fotoPerfil = bitmap;
                                imgPerfil.setImageBitmap(bitmap);

                                subirFotoPerfil(bitmap);
                            } else {
                                Log.e("CAMARA", "data no es Bitmap");
                            }
                        } else {
                            Log.e("CAMARA", "Extras null");
                        }
                    }
                }
        );

        imgPerfil.setOnClickListener(v -> abrirCamara());

        // Listener del menú lateral NavigationView
        nv.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            ListaRecetasFragment listaFragment =
                    (ListaRecetasFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);
            if(id == R.id.menu_agregar_receta) {
                Intent intent = new Intent(MainActivity.this, AgregarRecetaActivity.class);
                startForResult.launch(intent);
            } else if(id == R.id.menu_ver_recetas) { // todas
                if(listaFragment != null) {
                    listaFragment.mostrarRecetas();
                }
            } else if(id == R.id.menu_ver_recetas_favoritas) {
                if(listaFragment != null) {
                    listaFragment.mostrarFavoritas();
                }
            } else if(id == R.id.menu_supermercados) {
                Intent intent = new Intent(MainActivity.this, SupermercadosActivity.class);
                startActivity(intent);
            } else if(id == R.id.menu_logout) {
                sesion.cerrarSesion();
                Intent intent = new Intent(MainActivity.this, AuthUsuarioActivity.class);
                startActivity(intent);
                finish();
            }
            dl.closeDrawer(GravityCompat.START);
            return true;
        });

        // registro del launcher para recibir datos de AgregarRecetaActivity
        startForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // crear objeto Receta con los datos recibidos
                        Intent data = result.getData();
                        Receta r = new Receta();
                        r.titulo = data.getStringExtra("titulo");
                        r.categoria = data.getStringExtra("categoria");
                        r.tiempo = data.getIntExtra("tiempo", 0);
                        r.ingredientes = data.getStringExtra("ingredientes");
                        r.pasos = data.getStringExtra("pasos");
                        r.fotoUri = data.getStringExtra("fotoUri");

                        // guardar en db con id
                        long id = db.agregarReceta(r);
                        r.id = (int) id;

                        // actualizar la lista de recetas si está el fragmento
                        ListaRecetasFragment listaRecetasFragment =
                                (ListaRecetasFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_lista_recetas);

                        if (listaRecetasFragment != null) {
                            listaRecetasFragment.refreshLista();
                        }

                        // guardar la última receta añadida y lanzar notificación
                        ultimaRecetaAgregada = r;
                        lanzarNotificacion(ultimaRecetaAgregada);
                    }
                }
        );

        // inicializar db y botón de '+' (agregar receta)
        btnAgregar = findViewById(R.id.btnAgregarReceta);
        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AgregarRecetaActivity.class);
            startForResult.launch(intent);
        });

        // control del botón atrás para cerrar drawer o mostrar confirmación de salir de la app
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

    private void subirFotoPerfil(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "foto.jpg");

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            GestorSesionUsuario sesion = new GestorSesionUsuario(this);
            String email = sesion.getUserEmail();

            Data input = new Data.Builder()
                    .putString("email", email)
                    .putString("ruta_local", file.getAbsolutePath())
                    .build();

            OneTimeWorkRequest request =
                    new OneTimeWorkRequest.Builder(SubirImagenWorker.class)
                            .setInputData(input)
                            .build();

            WorkManager.getInstance(this).enqueue(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    200
            );
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureLauncher.launch(intent);
    }

    // Inflar menú para selección de idioma
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_idioma, menu);
        return true;
    }

    // Cambiar idioma según selección del menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_es) {
            GestorIdioma.setIdioma(this, "es");
            recreate();
            return true;
        } else if(id == R.id.menu_eu) {
            GestorIdioma.setIdioma(this, "eu");
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (GestorIdioma.languageChanged) {
            GestorIdioma.languageChanged = false;
            recreate();
        }
    }

    // Maneja la selección de receta desde la lista
    @Override
    public void onRecetaSeleccionada(Receta receta) {
        int orientation = getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) { // horizontal
            // mostrar receta en fragment de detalle
            DetalleRecetaFragment detalle = (DetalleRecetaFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_detalle_receta);
            if(detalle != null) {
                detalle.mostrarReceta(receta);
            }
        } else { // vertical
            // abrir actividad detalle
            Intent i = new Intent(this, DetalleRecetaActivity.class);
            i.putExtra("receta", receta);
            startActivity(i);
        }
    }

    // Solicitar eliminar receta desde lista
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

    // Confirmar eliminación de la receta
    private void eliminarRecetaConfirmada() {
        if(recetaEliminar != null) {
            db.eliminarReceta(recetaEliminar.id);
            ListaRecetasFragment lista = (ListaRecetasFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_lista_recetas);

            if(lista != null) {
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

    // Lanzar notificación de receta agregada
    private void lanzarNotificacion(Receta receta) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
                return;
            }
            enviarNotificacion(receta);
        } else {
            enviarNotificacion(receta);
        }
    }
    private void enviarNotificacion(Receta receta) {
        String canalId = "canal_recetas";
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal de notificación si es necesario
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalId,
                    "Canal Recetas",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            canal.setDescription("Notificaciones de recetas");
            manager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(this, DetalleRecetaActivity.class);
        intent.putExtra("receta", receta);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, canalId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(getString(R.string.noti_receta_agregada_titulo))
                        .setContentText(getString(R.string.noti_receta_agregada_texto, receta.titulo))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // Manejar permiso de notificaciones
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(this,
                        "Permiso de cámara denegado",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Eliminación de receta desde fragment de detalle
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
        // Si no está en landscape, cerrar la actividad detalle
        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            finish();
        }
    }

    // Notificación de receta eliminada
    private void lanzarNotificacionEliminada(String titulo) {
        String canalId = "canal_recetas";
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, canalId)
                        .setSmallIcon(android.R.drawable.ic_delete)
                        .setContentTitle(getString(R.string.noti_receta_eliminada_titulo))
                        .setContentText(getString(R.string.noti_receta_eliminada_texto, titulo))
                        .setSubText("Receta eliminada correctamente")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}