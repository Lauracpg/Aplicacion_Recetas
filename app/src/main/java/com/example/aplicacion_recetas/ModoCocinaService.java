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
    // acción para comunicar el cambio de paso a la activity
    public static final String ACTION_PASO = "modo_cocina_paso";
    public static final String EXTRA_INDEX = "index";

    // Binder para comunicación entre activity y service
    private final IBinder binder = new LocalBinder();

    // permite a la activity acceder al servicio directamente
    public class LocalBinder extends Binder {
        ModoCocinaService getService() {
            return ModoCocinaService.this;
        }
    }
    private Receta receta;
    private List<String> pasos;
    private int index = 0;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate();
        // el servicio arranca como foreground, se crea la notificación base
        crearNotificacionForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // recibe la receta al iniciar el servicio
        if (intent != null) {
            receta = (Receta) intent.getSerializableExtra("receta");
            if (receta != null) {
                pasos = parsePasos(receta.pasos);
            }
        }
        // mantiene el servicio en segundo plano
        crearNotificacionForeground();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int getIndex() {
        return index;
    }

    // avanza al siguiente paso de la receta
    public void siguientePaso() {
        if (receta == null) return;
        if (index < pasos.size() - 1) {
            index++;
            enviarBroadcast();
            actualizarNotificacion();
        }
    }

    // vuelve al paso anterior
    public void anteriorPaso() {
        if (index > 0) {
            index--;
            enviarBroadcast();
            actualizarNotificacion();
        }
    }

    // detiene el servicio foreground completamente
    public void detenerServicio() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    // notifica a la activity que el paso ha cambiado
    private void enviarBroadcast() {
        Intent broadcast = new Intent(ACTION_PASO);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_INDEX, index);
        sendBroadcast(broadcast);
    }

    // notifiación persistente del modo cocina
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

    // actualiza la notificación con el paso actual
    private void actualizarNotificacion() {
        Notification notification = new NotificationCompat.Builder(this, "cocina")
                .setContentTitle(getString(R.string.modo_cocina))
                .setContentText(pasos.get(index))
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        notificationManager.notify(1, notification);
    }

    // convierte el string de pasos en lista usable
    private List<String> parsePasos(String pasosRaw) {
        List<String> lista = new ArrayList<>();
        if(pasosRaw == null) return lista;

        // separa pasos numerados
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
