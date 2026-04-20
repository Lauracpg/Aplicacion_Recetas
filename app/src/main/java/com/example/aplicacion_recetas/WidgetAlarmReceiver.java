package com.example.aplicacion_recetas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class WidgetAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        // crear la entrada de datos para el worker
        Data input = new Data.Builder()
                .putString("accion", "obtener")
                .build();
        // crear trabajo em segundo plano con WorkManager, se ejecuta RecetasWorker para obtener datos del servidor
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(RecetasWorker.class)
                        .setInputData(input)
                        .build();
        WorkManager.getInstance(context).enqueue(request);
    }
}
