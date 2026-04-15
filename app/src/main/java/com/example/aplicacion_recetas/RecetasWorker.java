package com.example.aplicacion_recetas;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class RecetasWorker extends Worker {
    public RecetasWorker(@NonNull Context context,
                         @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        try {
            String accion = getInputData().getString("accion");

            URL url = new URL("http://34.175.70.22:81/recetas.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("accion", accion);

            if ("insertar".equals(accion)) {
                builder.appendQueryParameter("titulo", getInputData().getString("titulo"))
                        .appendQueryParameter("categoria", getInputData().getString("categoria"))
                        .appendQueryParameter("tiempo", String.valueOf(getInputData().getInt("tiempo", 0)))
                        .appendQueryParameter("ingredientes", getInputData().getString("ingredientes"))
                        .appendQueryParameter("pasos", getInputData().getString("pasos"))
                        .appendQueryParameter("fotoUri", getInputData().getString("fotoUri"));
            }

            String params = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(params);
            writer.flush();
            writer.close();
            os.close();

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            Log.d("RECETAS_API", json);
            JSONObject obj = new JSONObject(json);
            boolean success = obj.optBoolean("success", false);
            if (success) {
                return Result.success();
            } else {
                Log.e("RECETAS_API", obj.optString("error"));
                return Result.failure();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.failure();
    }
}
