package com.example.mygooglemaps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.AlertDialog;
import android.location.Address;
import android.location.Geocoder;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextView tvCoordinates;
    private EditText etSearch;
    private ImageButton btnCurrentLocation;
    private final List<LatLng> markerPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        tvCoordinates = findViewById(R.id.tvCoordinates);
        ImageButton btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        etSearch = findViewById(R.id.etSearch);
        ImageButton btnMapType = findViewById(R.id.btnMapType);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Button click listener
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnMapType.setOnClickListener(v -> showMapTypeDialog());

        // Search on keyboard "Enter"
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Set default location (Manila, Philippines)
        LatLng manila = new LatLng(14.5995, 120.9842);
        mMap.addMarker(new MarkerOptions()
                .position(manila)
                .title("Manila, Philippines"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 12));

        // Check and request permissions
        checkLocationPermission();

        // Long press to add markers and compute distance
        mMap.setOnMapLongClickListener(latLng -> {
            if (markerPoints.size() == 2) {
                markerPoints.clear();
                mMap.clear();
            }

            markerPoints.add(latLng);
            mMap.addMarker(new MarkerOptions().position(latLng).title("Point " + markerPoints.size()));

            if (markerPoints.size() == 2) {
                calculateDistance(markerPoints.get(0), markerPoints.get(1));
            }
        });
    }

    private void showMapTypeDialog() {
        String[] mapTypes = {"Normal", "Hybrid", "Satellite", "Terrain", "None"};
        new AlertDialog.Builder(this)
                .setTitle("Select Map Type")
                .setItems(mapTypes, (dialog, which) -> {
                    switch (which) {
                        case 0: mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); break;
                        case 1: mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); break;
                        case 2: mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); break;
                        case 3: mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN); break;
                        case 4: mMap.setMapType(GoogleMap.MAP_TYPE_NONE); break;
                    }
                }).show();
    }

    private void searchLocation() {
        String location = etSearch.getText().toString().trim();
        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(location, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                tvCoordinates.setText(String.format("Lat: %.4f, Lng: %.4f", latLng.latitude, latLng.longitude));
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateDistance(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(point1.latitude, point1.longitude,
                point2.latitude, point2.longitude, results);

        float distance = results[0]; // meters
        String distanceText = distance > 1000
                ? String.format(Locale.getDefault(), "Distance: %.2f km", distance / 1000)
                : String.format(Locale.getDefault(), "Distance: %.2f m", distance);

        Toast.makeText(this, distanceText, Toast.LENGTH_LONG).show();
    }
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            tvCoordinates.setText(String.format("Lat: %.4f, Lng: %.4f", latitude, longitude));

                            LatLng currentLocation = new LatLng(latitude, longitude);
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("You are here"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15), 2000, null);

                            Toast.makeText(MainActivity.this, "Location found!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
