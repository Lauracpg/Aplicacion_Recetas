package com.example.aplicacion_recetas;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class ModoCocinaService extends Service {
    public static final String ACTION_PASO = "modo_cocina_paso";
    public static final String EXTRA_INDEX = "index";

    private final IBinder binder = new LocalBinder();

    private Receta receta;
    private int index = 0;
    private List<String> pasos;
    private NotificationManager notificationManager;

    public int getIndex() {
        return index;
    }


    public class LocalBinder extends Binder {
        ModoCocinaService getService() {
            return ModoCocinaService.this;
        }
    }

    @Override
    public void onCreate() {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate();
        crearNotificacionForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            receta = (Receta) intent.getSerializableExtra("receta");
            if (receta != null) {
                pasos = parsePasos(receta.pasos);
            }
        }

        crearNotificacionForeground();
        return START_STICKY;
    }

    private void crearNotificacionForeground() {
        String textoInicial = (pasos != null && !pasos.isEmpty())
                ? pasos.get(0)
                : getString(R.string.cocinando);

        String channelId = "cocina";

        NotificationChannel channel = new NotificationChannel(
                channelId,
                getString(R.string.modo_cocina),
                NotificationManager.IMPORTANCE_LOW
        );

        notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.modo_cocina_activa))
                .setContentText(textoInicial)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void actualizarNotificacion() {
        Notification notification = new NotificationCompat.Builder(this, "cocina")
                .setContentTitle(getString(R.string.modo_cocina))
                .setContentText(pasos.get(index))
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        notificationManager.notify(1, notification);
    }

    public void siguientePaso() {
        if (receta == null) return;
        if (index < pasos.size() - 1) {
            index++;
            enviarBroadcast();
            actualizarNotificacion();
        }
    }

    public void anteriorPaso() {
        if (index > 0) {
            index--;
            enviarBroadcast();
            actualizarNotificacion();
        }
    }

    private void enviarBroadcast() {
        Intent broadcast = new Intent(ACTION_PASO);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_INDEX, index);
        sendBroadcast(broadcast);
    }

    public void detenerServicio() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private List<String> parsePasos(String pasosRaw) {
        List<String> lista = new ArrayList<>();
        if(pasosRaw == null) return lista;

        String[] partes = pasosRaw.split("\\d+\\.");

        for (String p : partes) {
            p = p.trim();
            if (!p.isEmpty()) {
                lista.add(p);
            }
        }
        return lista;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
