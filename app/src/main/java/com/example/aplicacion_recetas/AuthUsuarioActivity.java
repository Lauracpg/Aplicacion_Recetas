package com.example.aplicacion_recetas;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthUsuarioActivity extends AppCompatActivity {
    EditText editTextNombre, editTextEmail, editTextPassword;
    Button btnRegistrar, btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_usuario);

        // barra de estado y navegación del móvil
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        // config toolbar
        Toolbar toolbar = findViewById(R.id.toolbarLogin);
        setSupportActionBar(toolbar);

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnLogin = findViewById(R.id.btnLogin);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        btnLogin.setOnClickListener(v -> loginUsuario());

        // si el usuario está logueado, se salta esta pantalla
        GestorSesionUsuario sesion = new GestorSesionUsuario(this);
        if(sesion.estaLogueado()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    // inflar menú de idioma
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_idioma, menu);
        return true;
    }

    // cambio de idioma desde menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_es) {
            GestorIdioma.setIdioma(this, "es");
            recreate();
            return true;
        }
        if (item.getItemId() == R.id.menu_eu) {
            GestorIdioma.setIdioma(this, "eu");
            recreate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // registro de usuario
    private void registrarUsuario() {
        String nombre = editTextNombre.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // validar campos obligatorios
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    getString(R.string.nombre_email_password),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this,
                    getString(R.string.email_invalido),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // datos para el worker
        Data input = new Data.Builder()
                .putString(AppWorker.KEY_ACCION, "registrar")
                .putString(AppWorker.KEY_NOMBRE, nombre)
                .putString(AppWorker.KEY_EMAIL, email)
                .putString(AppWorker.KEY_PASSWORD, password)
                .build();

        // segundo plano
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AppWorker.class)
                .setInputData(input)
                .build();

        // evitar observers duplicados
        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .removeObservers(AuthUsuarioActivity.this);

        // observar el resultado del registro
        WorkManager.getInstance(AuthUsuarioActivity.this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(AuthUsuarioActivity.this, workInfo -> {
                    if (workInfo == null || !workInfo.getState().isFinished()) return;

                    if (workInfo.getState() == WorkInfo.State.FAILED) {
                        String error = workInfo.getOutputData().getString("error");
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(this, getString(R.string.registrado), Toast.LENGTH_SHORT).show();
                });
    }

    // login de usuario
    private void loginUsuario() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        // validar campos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    getString(R.string.email_password),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // validar email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this,
                    getString(R.string.email_invalido),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // datos para worker
        Data input = new Data.Builder()
                .putString(AppWorker.KEY_ACCION, "login")
                .putString(AppWorker.KEY_EMAIL, email)
                .putString(AppWorker.KEY_PASSWORD, password)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AppWorker.class)
                .setInputData(input)
                .build();
        // evita observers duplicados
        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .removeObservers(this);

        // observar el resultado del login
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo == null || !workInfo.getState().isFinished()) return;
                    // error en backend o conexión
                    if (workInfo.getState() == WorkInfo.State.FAILED) {
                        String error = workInfo.getOutputData().getString("error");
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String response = workInfo.getOutputData().getString("response");
                    if (response == null || response.isEmpty()) {
                        Toast.makeText(this, "Respuesta vacía del servidor", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        // parseo de respuesta json del servidor
                        JSONObject json = new JSONObject(response);
                        if(json.getBoolean("success")) {
                            // datos del usuario autenticado
                            int id = json.getInt("id");
                            String nombre = json.getString("nombre");
                            String emailServer = json.optString("email", email);
                            String foto = json.optString("foto", null);

                            // guardar sesión local
                            GestorSesionUsuario sesion = new GestorSesionUsuario(AuthUsuarioActivity.this);
                            sesion.guardarUsuario(id, nombre, emailServer);
                            if (foto != null && !foto.isEmpty()) {
                                sesion.guardarFoto(foto);
                            }

                            // acceso a la app
                            Toast.makeText(AuthUsuarioActivity.this, getString(R.string.bienvenido) + " " + nombre, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(AuthUsuarioActivity.this, MainActivity.class));
                            finish();

                        } else { // mensaje de error del sevidor
                            Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parseando respuesta", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}