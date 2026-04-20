package com.example.aplicacion_recetas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SupermercadosActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private GoogleMap mMap;
    private Marker userMarker;
    private final List<Marker> supermercados = new ArrayList<>();
    private LatLng lastSearchLocation = null;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supermercados);

        // inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // toolbar superior
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.supermercados_cerca);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // cliente de localización GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if(locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if(location != null) {
                    LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                    updateUser(pos);
                    buscarSupermercados(pos);
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        requestLocationPermission(); // pide permisos de ubicación
    }

    private void requestLocationPermission() {
        // si no hay permiso, lo solicita
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            enableUserLocation(); // si ya está concedido
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enableUserLocation();
        }
    }

    private void enableUserLocation() {
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // activa el punto azul del usuario en el mapa
        mMap.setMyLocationEnabled(true);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        // obtiene la última ubicación conocida
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );
    }

    private void updateUser(LatLng pos) {
        if(userMarker != null) {
            userMarker.remove(); // eliminar anterior
        }
        // crear marcador del usuario
        userMarker = mMap.addMarker(new MarkerOptions()
                .position(pos).title(getString(R.string.ubicacion))
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_VIOLET))
        );

        // mueve la cámara al usuario
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }

    private void buscarSupermercados(LatLng location) {
        // evita volver a buscar si el usuario no se ha movido mucho
        if(lastSearchLocation != null && distanciaEntre(lastSearchLocation, location) < 200) {
            return;
        }

        lastSearchLocation = location;
        clearSupermercados();

        String apiKey = getString(R.string.maps_api_key);
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + location.latitude + "," + location.longitude +
                "&radius=1500" +
                "&type=supermarket" +
                "&key=" + apiKey;

        // petición HTTP en segundo plano
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.connect();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                JSONObject obj = new JSONObject(json.toString());

                JSONArray results = obj.getJSONArray("results");

                if (results == null) {
                    return;
                }

                // pintar supermercados en el mapa
                runOnUiThread(() -> {
                    for (int i = 0; i < results.length(); i++) {
                        try{
                            JSONObject place = results.getJSONObject(i);
                            JSONObject loc = place.getJSONObject("geometry")
                                    .getJSONObject("location");

                            LatLng supermercado = new LatLng(
                                    loc.getDouble("lat"),
                                    loc.getDouble("lng")
                            );
                            String name = place.getString("name");

                            Marker marker = mMap.addMarker(
                                    new MarkerOptions().position(supermercado).title(name)
                            );

                            supermercados.add(marker);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // distancia entre puntos
    private float distanciaEntre(LatLng a, LatLng b) {
        float[] result = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result);
        return result[0];
    }

    // limpiar marcadores
    private void clearSupermercados() {
        for (Marker m : supermercados) {
            m.remove();
        }
        supermercados.clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

