package com.example.aplicacion_recetas;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ModoCocinaActivity extends AppCompatActivity {
    private TextView txtPaso, txtTitulo, txtProgreso;
    private Button btnSiguiente, btnAnterior, btnSalir;
    private Receta receta;
    private List<String> pasos; // lista de pasos ya procesados

    private ModoCocinaService servicio; // servicio de modo cocina
    private boolean bound = false;
    private boolean receiverRegistered = false; // control de registro del BroadcastReceiver

    // recibe actualizaciones del servicio (cambio de paso)
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ModoCocinaService.ACTION_PASO.equals(intent.getAction())) {
                int index = intent.getIntExtra(
                        ModoCocinaService.EXTRA_INDEX,
                        0
                );
                mostrarPaso(index);
            }
        }
    };

    // conexión al servicio de modo cocina
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ModoCocinaService.LocalBinder b = (ModoCocinaService.LocalBinder) binder;
            servicio = b.getService();
            bound = true;
            // sincroniza la UI con el estado actual del servicio
            mostrarPaso(servicio.getIndex());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            servicio = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_cocina);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        txtTitulo = findViewById(R.id.txtTitulo);
        txtPaso = findViewById(R.id.txtPaso);
        txtProgreso = findViewById(R.id.txtProgreso);

        btnSiguiente = findViewById(R.id.btnSiguiente);
        btnAnterior = findViewById(R.id.btnAnterior);
        btnSalir = findViewById(R.id.btnSalir);

        // mantener pantalla encendida durante la receta
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // ocultar action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // recibir receta desde la actividad anterior
        receta = (Receta) getIntent().getSerializableExtra("receta");
        if (receta == null) {
            finish();
            return;
        }

        txtTitulo.setText(receta.titulo);

        // convertir pasos en lista legible
        pasos = parsePasos(receta.pasos);

        // navegación de pasos mediante servicio
        btnSiguiente.setOnClickListener(v -> {
            if (bound && servicio != null) {
                servicio.siguientePaso();
            }
        });

        btnAnterior.setOnClickListener(v -> {
            if (bound && servicio != null) {
                servicio.anteriorPaso();
            }
        });

        btnSalir.setOnClickListener(v -> {
            // detiene el servicio al salir del modo cocina
            if (bound && servicio != null) {
                servicio.detenerServicio();
            }

            if (bound) {
                unbindService(connection);
                bound = false;
            }
            finish();
        });

        // iniciar servicio en foreground
        Intent intent = new Intent(this, ModoCocinaService.class);
        intent.putExtra("receta", receta);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        // vincularse al servicio
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // registrar receiver para recibir cambios de paso
        IntentFilter filter = new IntentFilter(ModoCocinaService.ACTION_PASO);

        ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        receiverRegistered = true;

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (receiverRegistered) {
            unregisterReceiver(receiver);
            receiverRegistered = false;
        }

        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    // muestra el paso actual y actualiza controles
    private void mostrarPaso(int index) {
        if (pasos == null || pasos.isEmpty()) return;

        txtPaso.setText(pasos.get(index));

        txtProgreso.setText(
                getString(R.string.paso_formato, index + 1, pasos.size())
        );

        btnAnterior.setEnabled(index > 0);
        btnSiguiente.setEnabled(index < pasos.size() - 1);

        btnAnterior.setAlpha(index > 0 ? 1f : 0.5f);
        btnSiguiente.setAlpha(index < pasos.size() - 1 ? 1f : 0.5f);
    }

    // convierte el string de pasos en lista limpia
    private List<String> parsePasos(String pasosRaw) {
        List<String> lista = new ArrayList<>();
        if(pasosRaw == null) return lista;

        // separa por numeración
        String[] partes = pasosRaw.split("\\d+\\.");

        for (String p : partes) {
            p = p.trim();
            if (!p.isEmpty()) {
                lista.add(p);
            }
        }
        return lista;
    }
}
