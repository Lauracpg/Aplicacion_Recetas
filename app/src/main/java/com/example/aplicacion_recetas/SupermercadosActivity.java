package com.example.aplicacion_recetas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;

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
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;

    private Marker userMarker;
    private final List<Marker> supermercados = new ArrayList<>();
    private LatLng lastSearchLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.aplicarIdioma(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supermercados);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_idioma, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_es) {
            GestorIdioma.setIdioma(this, "es");
            recreate();
            return true;
        } else if(id == R.id.menu_eu) {
            GestorIdioma.setIdioma(this, "eu");
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            enableUserLocation();
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

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {

                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                updateUser(pos);
                buscarSupermercados(pos);
                lastSearchLocation = pos;
            }
        });
    }

    private void updateUser(LatLng pos) {
        if(userMarker != null) {
            userMarker.remove();
        }
        userMarker = mMap.addMarker(new MarkerOptions()
                .position(pos).title(getString(R.string.ubicacion))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }

    private void buscarSupermercados(LatLng location) {

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

    private float distanciaEntre(LatLng a, LatLng b) {
        float[] result = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result);
        return result[0];
    }


    private void clearSupermercados() {
        for (Marker m : supermercados) {
            m.remove();
        }
        supermercados.clear();
    }
}

