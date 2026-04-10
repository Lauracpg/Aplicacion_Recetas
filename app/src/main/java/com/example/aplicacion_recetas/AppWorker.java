package com.example.aplicacion_recetas;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AppWorker extends Worker {
    public static final String KEY_ACCION = "accion";
    public static final String KEY_NOMBRE = "nombre";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";

    public AppWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        String accion = getInputData().getString(KEY_ACCION);
        if (accion == null) return Result.failure();
        try {
            URL url;
            if ("registrar".equals(accion)) {
                url = new URL("http://34.175.31.18:81/registro.php");
            } else if ("login".equals(accion)) {
                url = new URL("http://34.175.31.18:81/login.php");
            } else {
                return Result.failure();
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject json = new JSONObject();
            if ("registrar".equals(accion)) {
                json.put("nombre", getInputData().getString(KEY_NOMBRE));
                json.put("email", getInputData().getString(KEY_EMAIL));
                json.put("password", getInputData().getString(KEY_PASSWORD));
            } else if ("login".equals(accion)) {
                json.put("email", getInputData().getString(KEY_EMAIL));
                json.put("password", getInputData().getString(KEY_PASSWORD));
            }

            OutputStream os = conn.getOutputStream();
            os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream())
            );
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();

            Data outputData = new Data.Builder()
                    .putString("response", result.toString())
                    .build();
            return Result.success(outputData);

        } catch (Exception e) {
            Log.e("HTTP_ERROR", "Error em conexión", e);
            return Result.failure(new Data.Builder()
                            .putString("error", e.getMessage())
                            .build()
            );
        }
    }
}