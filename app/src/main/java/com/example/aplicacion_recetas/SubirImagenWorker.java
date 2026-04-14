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
import java.io.InputStream;
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
            String email = getInputData().getString("email");
            String rutaLocal = getInputData().getString("ruta_local");

            if (email == null || rutaLocal  == null) {
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

            byte[] bytes = buffer.toByteArray();

            String imagenBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);

            URL url = new URL("http://34.175.70.22:81/subir_foto.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("email", email)
                    .appendQueryParameter("imagen", imagenBase64);

            String params = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
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

                boolean success = json.optBoolean("success", false);
                String ruta = json.optString("ruta", null);

                if (success && ruta != null) {
                    GestorSesionUsuario sesion =
                            new GestorSesionUsuario(getApplicationContext());

                    sesion.guardarFoto(ruta);

                    Data output = new Data.Builder()
                            .putString("ruta", ruta)
                            .build();

                    return Result.success(output);
                }
            }
            return Result.failure();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
