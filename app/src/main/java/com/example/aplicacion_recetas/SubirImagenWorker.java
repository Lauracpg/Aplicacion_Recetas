package com.example.aplicacion_recetas;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SubirImagenWorker extends Worker {
    public SubirImagenWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        try {
            String tipo = getInputData().getString("tipo"); //foto de perfil o foto de receta
            String rutaLocal = getInputData().getString("ruta_local");

            if (tipo == null || rutaLocal  == null) {
                return Result.failure();
            }

            File file = new File(rutaLocal);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(file);

            byte[] data = new byte[1024];
            int n;
            while ((n = fis.read(data)) != -1) {
                buffer.write(data, 0, n);
            }
            fis.close();

            String imgBase64 = Base64.encodeToString(buffer.toByteArray(), Base64.DEFAULT);

            URL url = new URL("http://34.175.70.22:81/subir_foto.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("tipo", tipo)
                    .appendQueryParameter("imagen", imgBase64);

            if("perfil".equals(tipo)) {
                String email = getInputData().getString("email");
                builder.appendQueryParameter("email", email);

            } else if("receta".equals(tipo)) {
                int id = getInputData().getInt("id", 0);
                int idUsuario = getInputData().getInt("idUsuario", 0);
                builder.appendQueryParameter("id", String.valueOf(id));
                builder.appendQueryParameter("idUsuario", String.valueOf(idUsuario));
            }

            String params = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            JSONObject json = new JSONObject(response.toString());
            if(json.optBoolean("success")) {
                String ruta = json.optString("ruta", null);
                if("perfil".equals(tipo) && ruta != null) {
                    new GestorSesionUsuario(getApplicationContext()).guardarFoto(ruta);
                }
                return Result.success(
                        new Data.Builder()
                                .putString("ruta", ruta)
                                .build()
                );
            }
            return Result.failure();

        } catch (Exception e) {
            return Result.failure();
        }
    }
}
