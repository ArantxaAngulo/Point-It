package com.akranka10.campusnavigationapp01;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.OnSuccessListener;
import android.location.Location;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.location.Location;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    //List<LatLng> campusLocations = new ArrayList<>();
    Map<String, LatLng> poiMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //initializing client location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // Make sure your XML layout has a fragment with this ID

    }

    //my code
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap; //new Google Map object

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Enable "My Location" button on the map
        mMap.setMyLocationEnabled(true);

        // Get the last known location and update the map camera
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Convert location to LatLng and move the camera
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                    //POIs
                    // Setting POIs (Points of Interest) with LatLng objects, to set coordinates
                        //poiMap.put("Library", new LatLng(20.605873, -103.415513));
                    poiMap.put("El Hueco", new LatLng(20.6049088, -103.4153376));
                    poiMap.put("Tacos El Ojitos", new LatLng(20.6083417, -103.4326965));
                    poiMap.put("Charlie Boy Burgers & Shakes", new LatLng(20.628596, -103.409205));
                    poiMap.put("Hamburguesas Beto's", new LatLng(20.641518, -103.420175));
                    poiMap.put("Mile Pizzas a la Lena", new LatLng(20.641990,-103.401016));
                    poiMap.put("Casa Macaria Cruz del Sur", new LatLng(20.642652, -103.387851));
                    poiMap.put("Pizzas D'TERE", new LatLng(20.650405, -103.432099));
                    poiMap.put("Asador Santa Anita", new LatLng(20.560811, -103.448668));

                    // Adding markers of POIs to the map
                    for (Map.Entry<String, LatLng> entry : poiMap.entrySet()) {
                        mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.dot_icon))
                                .position(entry.getValue())
                                .title(entry.getKey()));
                    }

                    // Retrieve the nearest 5 POIs
                    List<POI> nearestPOIs = findNearestPOIs(userLocation.latitude, userLocation.longitude, 5);

                    // Add markers for each of the nearest POIs
                    for (POI poi : nearestPOIs) {
                        mMap.addMarker(new MarkerOptions().position(poi.location).title(poi.name));
                    }

                    // Create a string array for the names of the nearest POIs
                    String[] poiNames = new String[nearestPOIs.size()];
                    for (int i = 0; i < nearestPOIs.size(); i++) {
                        poiNames[i] = nearestPOIs.get(i).name; // Get the names of POIs
                    }

                    // Create and display an AlertDialog with the list of POIs
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Nearest POIs")
                            .setItems(poiNames, null)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Call to super method
        super.onRequestPermissionsResult(requestCode,permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, reattempt to set up the location tracking
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        }
                    });
                }
            } else {
                // Permission denied;
            }
        }
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radius of Earth in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public List<POI> findNearestPOIs(double userLat, double userLng, int k) {
        // Map to hold distances and corresponding POIs
        List<POI> pois = new ArrayList<>(poiMap.size());
        // Define a maximum distance (in kilometers)
        double maxDistanceKm = 5.0;

        // Populate the list with POIs from map
        for (Map.Entry<String, LatLng> entry : poiMap.entrySet()) {
            double distance = calculateDistance(userLat, userLng, entry.getValue().latitude, entry.getValue().longitude);

            // Check if the POI is within the maximum distance
            if (distance <= maxDistanceKm) {
                pois.add(new POI(entry.getKey(), entry.getValue())); // Add the POI to the list
            }
        }

        // Sort POIs based on distance from user location
        pois.sort(Comparator.comparingDouble(poi -> calculateDistance(userLat, userLng, poi.location.latitude, poi.location.longitude)));

        return pois; // Return the list of nearest POIs
    }

    // Helper class to store POI and its distance
    public class POI {
        String name;
        LatLng location;

        public POI(String name, LatLng location) {
            this.name = name;
            this.location = location;
        }
    }



}

