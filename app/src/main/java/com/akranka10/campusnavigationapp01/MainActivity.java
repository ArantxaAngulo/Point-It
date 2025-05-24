package com.akranka10.campusnavigationapp01;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.os.Handler;
import android.os.Looper;
import com.google.android.libraries.places.api.net.PlacesClient;
import android.location.Location;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Main activity for the Points of Interest (POI) application.
 * This class handles map initialization, location services, and POI discovery.
 *
 * The application allows users to:
 * - View their current location on a Google Map
 *  - Add custom POIs by clicking on the map or by address
 * - Find nearest Points of Interest within a 5 km radius
 * - Explore different types of POIs (Restaurants, Historical Sites, Parks)
 *
 * @author [Arantxa]
 * @version v0.5.4
 * @since [11/29/2024]
 */

// "MAIN" CLASS
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Global variables
    private POILocalStorage localStorage;
    private List<POI> allPOIs = new ArrayList<>(); // Saves POI objects
    private Map<String, Marker> poiMarkers = new HashMap<>(); //manages and relates POIs to markers

    private boolean isAddingPOI = false; //flag
    private Button addPOIButton;

    private Button addPOIByAddressButton;
    private PlacesClient placesClient;
    private boolean isAddingByAddress = false;

    private boolean isDeleteMode = false; //flag
    private Button deletePOIButton;

    private static List<POIList> poiLists = new ArrayList<>();
    private ArrayAdapter<POIList> listsAdapter;

    private boolean isAddToListMode = false;
    private Button addToListButton;

    private Spinner poiListSpinner;
    private ArrayAdapter<POIList> listSpinnerAdapter;
    private POIList currentFilterList = null;

    private GoogleMap mMap; // New Object GoogleMap
    private FusedLocationProviderClient fusedLocationClient; // Object FusedLocationProviderClient
    Map<String, LatLng> poiMap = new HashMap<>(); // HashMap named poiMap that relates PLACENAME String to COORDINATES LatLng // LatLng Datatype provided by google
    private LatLng userLocation; // Coordinates to store user location
    private static final long UPDATE_INTERVAL_MS = 5000; // 5 seconds
    private static final float PROXIMITY_RADIUS_METERS = 800; // 200m trigger radius
    private LocationCallback locationCallback;

    private long lastNotificationTime = 0;
    private static final long NOTIFICATION_COOLDOWN_MS = 300000; // 5 minutes

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "proximity_alerts",
                    "Nearby Places",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for nearby saved locations");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d("NOTIFICATION", "Notification channel created");
        }
    }
    
    /**
     * Initializes the activity, sets up the user interface, and configures map and location services.
     *
     * This method:
     * - Sets the content view
     * - Initializes buttons
     * - Initializes the Google Map with its dependencies
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        // Initialize local storage
        localStorage = new POILocalStorage(this);

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        // Initialize the drawer layout and FAB
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        FloatingActionButton fab = findViewById(R.id.fab_add_poi);
        fab.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Initialize the "Add POI by Address" button
        addPOIByAddressButton = findViewById(R.id.add_poi_by_address_button);
        addPOIByAddressButton.setOnClickListener(v -> toggleAddressInputMode());

        // Initialize "Find nearest POIs" button
        Button findNearestButton = findViewById(R.id.find_nearest_button); // Variable "findNearestButton" of type Button is equal to ID find_nearest_button
        findNearestButton.setOnClickListener(v -> findNearestPOIsAndDisplay()); // Whenever the button is clicked, view resets

        // Initialize "Add POI" (by clicking) button
        addPOIButton = findViewById(R.id.add_poi_button);
        addPOIButton.setOnClickListener(v -> togglePOIAddingMode());

        //Initialize "Delete POI" button
        deletePOIButton = findViewById(R.id.delete_poi_button);
        deletePOIButton.setOnClickListener(v -> {
            isDeleteMode = !isDeleteMode;
            if (isDeleteMode) {
                deletePOIButton.setText("Cancel");
                deletePOIButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                Toast.makeText(this, "Tap a POI to delete it", Toast.LENGTH_SHORT).show();
            } else {
                deletePOIButton.setText("Delete POI");
                deletePOIButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            }
        });

        //Initialize Add POI to list button
        addToListButton = findViewById(R.id.add_to_list_button);
        addToListButton.setOnClickListener(v -> toggleAddToListMode());

        Button toggleFilterButton = findViewById(R.id.toggle_filter_button);
        toggleFilterButton.setOnClickListener(v -> {
            if (poiListSpinner.getVisibility() == View.VISIBLE) {
                poiListSpinner.setVisibility(View.GONE);
                showAllPOIs();
            } else {
                poiListSpinner.setVisibility(View.VISIBLE);
            }
        });

        // Initialize delete poi list button
        Button deleteListButton = findViewById(R.id.btn_delete_list);
        deleteListButton.setOnClickListener(v -> showDeleteListDialog());

        // Initializing client location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initalizing continuos location updates for notificactions
        createLocationCallback();
        startLocationUpdates();

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        // Initializing drawer
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Apply padding to drawer content
            View drawerContent = findViewById(R.id.nav_view);
            drawerContent.setPadding(
                    drawerContent.getPaddingStart(),
                    systemBars.top + 16,  // Status bar height + 16dp extra
                    drawerContent.getPaddingEnd(),
                    systemBars.bottom + 16 // Navigation bar height + 16dp extra
            );

            // Also apply insets to main content if needed
            View mainContent = findViewById(R.id.main_content);
            mainContent.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return insets;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initalize Custom POI List Drawer
        ListView listsView = findViewById(R.id.list_poi_lists);
        Button createListButton = findViewById(R.id.btn_create_list);

        listsAdapter = new ArrayAdapter<POIList>(this,
                android.R.layout.simple_list_item_1, poiLists) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(poiLists.get(position).getName());
                return view;
            }
        };
        listsView.setAdapter(listsAdapter);

        // Set up button and list view listeners
        createListButton.setOnClickListener(v -> showCreateListDialog());
        listsView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d("DEBUG", "List item clicked at position: " + position);
            POIList selectedList = poiLists.get(position);
            Log.d("DEBUG", "Selected list: " + (selectedList != null ? selectedList.getName() : "null"));
            if (selectedList.isShowAll()) {
                Log.e("DEBUG", "Selected list is null!");
                showAllPOIs();
            } else {
                Log.d("DEBUG", "Showing details for: " + selectedList.getName());
                showPOIListDetails(selectedList); // This shows the dialog with delete option
            }

            // Show a toast to confirm the click was registered
            Toast.makeText(this, "Clicked: " + (selectedList != null ? selectedList.getName() : "null"), Toast.LENGTH_SHORT).show();
        });

        // Initialize UI components FIRST
        poiListSpinner = findViewById(R.id.poi_list_spinner);

        // Initialize spinner adapter
        listSpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        listSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        poiListSpinner.setAdapter(listSpinnerAdapter);

        loadPOILists();
        Log.d("DEBUG", "Loaded lists: " + poiLists.size());
        for (POIList list : poiLists) {
            Log.d("DEBUG", "List: " + list.getName() + " isShowAll: " + list.isShowAll());
        }

        poiListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                POIList selectedList = (POIList) parent.getItemAtPosition(position);
                currentFilterList = selectedList;
                if (selectedList.isShowAll()) {
                    showAllPOIs();
                } else {
                    filterPOIsByList(selectedList);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                showAllPOIs();
            }
        });
    }

    /**
     * Callback method triggered when the Google Map is ready to be used.
     *
     * This method:
     * - Actually sets up the map object after initialization
     * - Requests location permissions
     * - Retrieves the user's last known location
     * - Moves the camera to the user's location
     * - Initializes Points of Interest
     *
     * @param googleMap The GoogleMap object representing the map
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap; // GoogleMap Object
        mMap.getUiSettings().setAllGesturesEnabled(true); //UI

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permissions if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Enable Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        // Enable "My Location" button
        mMap.setMyLocationEnabled(true);

        // Clear any existing click listeners first
        mMap.setOnMapClickListener(null);

        // Add map click listener for POI creation
        mMap.setOnMapClickListener(latLng -> {
            Toast.makeText(this, "Map clicked at: " + latLng.toString(), Toast.LENGTH_SHORT).show(); //debug
            Log.d("POI_DEBUG", "Basic click test worked"); //debug

            if (isAddingPOI) {
                Log.d("POI_DEBUG", "Attempting to show dialog"); //debug
                runOnUiThread(() -> {
                    try {
                        showPOICreationDialog(latLng); //Calls POI creation method
                        Log.d("POI_DEBUG", "Dialog shown successfully"); //debug
                    } catch (Exception e) {
                        Log.e("POI_DEBUG", "Dialog error", e); //debug
                    }
                });
            }
        });

        // Add marker click listener for POI show and deletion
        mMap.setOnMarkerClickListener(marker -> {
            // Find the POI associated with this marker
            for (Map.Entry<String, Marker> entry : poiMarkers.entrySet()) { // iterating over each string,marker pairs in poiMarkers
                if (entry.getValue().equals(marker)) {
                    String poiId = entry.getKey();
                    POI poi = findPOIById(poiId);
                    if (poi != null) { // If a POI id is found
                        if (isDeleteMode) {
                            // Show confirmation dialog for deletion
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Delete POI")
                                    .setMessage("Are you sure you want to delete " + poi.getName() + "?")
                                    .setPositiveButton("Delete", (dialog, which) -> {
                                        deleteUserPOI(poi);
                                        isDeleteMode = false;
                                        deletePOIButton.setText("Delete POI");
                                        deletePOIButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_blue_dark));
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return true;
                        }
                        else if (isAddToListMode){
                            // Show add to list dialog immediately
                            showAddToListDialog(poi);
                            return true;
                        } else {
                            // Default behavior - just show info window
                            marker.showInfoWindow();
                            return false;
                        }
                    }
                }
            }
            return false;
        });

        // Get the last known location and update the map camera
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {

            @Override
            public void onSuccess(Location location) { // Method onSuccess that rececives location
                if (location != null) { // While the location is given...

                    userLocation = new LatLng(location.getLatitude(), location.getLongitude()); // Convert location to LatLng Object and save it to userLocation
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15)); // Move the camera to userLocation

                }
            }
        });

        // Load and display all POIs when map is ready
        initializePOIMap();
        showAllPOIs();  // Ensure POIs are visible at startup
    }

    /**
     * Toggles add POI to List on/off
     */
    private void toggleAddToListMode() {
        isAddToListMode = !isAddToListMode;
        if (isAddToListMode) {
            addToListButton.setText("Cancel");
            addToListButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
            Toast.makeText(this, "Tap a POI to add it to lists", Toast.LENGTH_SHORT).show();
        } else {
            addToListButton.setText("Add to List");
            addToListButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    /**
     * Toggles the address input mode on/off
     */
    private void toggleAddressInputMode() {
        isAddingByAddress = !isAddingByAddress; //flag turns true

        if (isAddingByAddress) {
            isAddingPOI = false; // Reset other adding modes
            addPOIButton.setText("Add POI");
            addPOIButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));

            // Set this button's state
            addPOIByAddressButton.setText("Cancel");
            addPOIByAddressButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            showAddressInputDialog();
        } else {
            // Reset this button's state
            addPOIByAddressButton.setText("Add POI by Address");
            addPOIByAddressButton.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
        }
    }

    /**
     * Toggles the POI adding mode on/off
     */
    private void togglePOIAddingMode() {
        isAddingPOI = !isAddingPOI;

        if (isAddingPOI) {
            addPOIButton.setText("Cancel Adding POI");
            addPOIButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(this, "Tap on the map to add a POI", Toast.LENGTH_SHORT).show();
        } else {
            addPOIButton.setText("Add POI");
            addPOIButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
            Toast.makeText(this, "POI adding mode disabled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  Handles lifecycle events to conserve battery
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(
                new LocationRequest.Builder(UPDATE_INTERVAL_MS)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build(),
                locationCallback,
                Looper.getMainLooper()
        );
    }
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void onListUpdated() {
        loadPOILists();
        if (currentFilterList != null) {
            // Refresh filter if one was active
            currentFilterList = poiLists.stream()
                    .filter(list -> list != null && list.getId().equals(currentFilterList.getId()))
                    .findFirst()
                    .orElse(null);

            if (currentFilterList != null) {
                filterPOIsByList(currentFilterList);
            } else {
                showAllPOIs();
            }
        }
    }

    private void loadPOILists() {
        if (poiListSpinner == null || listSpinnerAdapter == null) {
            Log.e("MainActivity", "Spinner or adapter not initialized!");
            return;
        }
        // Clear existing lists
        poiLists.clear();

        // Load saved lists first
        List<POIList> savedLists = localStorage.loadPOILists();
        poiLists.addAll(savedLists);

        // Add "Show All" option at beginning if it doesn't exist
        boolean hasShowAll = false;
        for (POIList list : poiLists) {
            if (list.isShowAll()) {
                hasShowAll = true;
                break;
            }
        }
        if (!hasShowAll) {
            poiLists.add(0, new POIList("Show All POIs", "Shows all points of interest", true));
        }

        // Create a new adapter to ensure complete refresh
        listSpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, poiLists);
        listSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        poiListSpinner.setAdapter(listSpinnerAdapter);

        // Restore selection if possible
        if (currentFilterList != null) {
            for (int i = 0; i < poiLists.size(); i++) {
                if (poiLists.get(i).getId().equals(currentFilterList.getId())) {
                    poiListSpinner.setSelection(i);
                    break;
                }
            }
        }

    }

    private void showCreateListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New POI List");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText nameInput = new EditText(this);
        nameInput.setHint("List Name");
        layout.addView(nameInput);

        EditText descInput = new EditText(this);
        descInput.setHint("Description (optional)");
        layout.addView(descInput);

        builder.setView(layout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        String desc = descInput.getText().toString().trim();
                        POIList newList = new POIList(name, desc);
                        localStorage.addPOIList(newList);
                        loadPOILists();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterPOIsByList(POIList list) {
        // Clear all markers first
        clearAllMarkers();

        // Add only POIs from the selected list
        for (String poiId : list.getPoiIds()) {
            POI poi = findPOIById(poiId);
            if (poi != null) {
                addPOIToMap(poi);
            }
        }

        Toast.makeText(this, "Showing POIs from: " + list.getName(), Toast.LENGTH_SHORT).show();
    }

    private void showAllPOIs() {
        clearAllMarkers();
        for (POI poi : allPOIs) {
            addPOIToMap(poi);
        }
        if (mMap != null) {
            if (userLocation != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            }
        }
        Toast.makeText(this, "Showing all POIs", Toast.LENGTH_SHORT).show();
    }

    private void clearAllMarkers() {
        for (Marker marker : poiMarkers.values()) {
            marker.remove();
        }
        poiMarkers.clear();
    }

    /**
     * Method that helps define thecustom POI list details
     * @param list
     */
    private void showPOIListDetails(POIList list) {
        if (list == null || list.isShowAll()) {
            showAllPOIs();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(list.getName());
        builder.setMessage(list.getDescription());

        // Get POIs in this list
        List<POI> poisInList = new ArrayList<>();
        for (String poiId : list.getPoiIds()) {
            POI poi = findPOIById(poiId);
            if (poi != null) {
                poisInList.add(poi);
            }
        }

        if (poisInList.isEmpty()) {
            builder.setMessage("No POIs in this list yet");
        } else {
            String[] poiNames = new String[poisInList.size()];
            for (int i = 0; i < poisInList.size(); i++) {
                poiNames[i] = poisInList.get(i).getName();
            }
            builder.setItems(poiNames, null);
        }

        // THREE BUTTONS: Close, Add POIs, and DELETE LIST
        builder.setPositiveButton("Close", null)
                .setNeutralButton("Add POIs", (dialog, which) -> {
                    showAddPOIsToListDialog(list);
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Delete for poi lists
     */
    private void showDeleteListDialog() {
        List<POIList> deletableLists = new ArrayList<>();
        for (POIList list : poiLists) {
            if (!list.isShowAll()) { // Don't allow deleting the "Show All" list
                deletableLists.add(list);
            }
        }

        if (deletableLists.isEmpty()) {
            Toast.makeText(this, "No lists available to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] listNames = new String[deletableLists.size()];
        for (int i = 0; i < deletableLists.size(); i++) {
            listNames[i] = deletableLists.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select List to Delete")
                .setItems(listNames, (dialog, which) -> {
                    POIList listToDelete = deletableLists.get(which);
                    showDeleteListConfirmation(listToDelete);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showDeleteListConfirmation(POIList list) {
        new AlertDialog.Builder(this)
                .setTitle("Delete List")
                .setMessage("Are you sure you want to delete the list '" + list.getName() + "'?\n\n" +
                        "Note: The POIs themselves will not be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteList(list);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void deleteList(POIList list) {
        // Remove from storage
        localStorage.removePOIList(list.getId());

        // Remove from our in-memory list
        poiLists.remove(list);

        // Update the spinner adapter
        listSpinnerAdapter.notifyDataSetChanged();

        // If we were viewing this list, show all POIs
        if (currentFilterList != null && currentFilterList.getId().equals(list.getId())) {
            currentFilterList = null;
            showAllPOIs();
        }

        Toast.makeText(this, "List deleted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Works with showpoilistdetails
     * @param list
     */
    private void showAddPOIsToListDialog(POIList list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add POIs to " + list.getName());

        // Get all available POIs
        List<POI> allPOIs = localStorage.loadUserPOIs();

        if (allPOIs.isEmpty()) {
            builder.setMessage("No POIs available to add");
            builder.setPositiveButton("OK", null);
        } else {
            String[] poiNames = new String[allPOIs.size()];
            boolean[] checkedItems = new boolean[allPOIs.size()];

            // Check which POIs are already in the list
            for (int i = 0; i < allPOIs.size(); i++) {
                poiNames[i] = allPOIs.get(i).getName();
                checkedItems[i] = list.getPoiIds().contains(allPOIs.get(i).getId());
            }

            builder.setMultiChoiceItems(poiNames, checkedItems, (dialog, which, isChecked) -> {
                // This just tracks selections, we'll handle changes in the button click
            });

            builder.setPositiveButton("Save", (dialog, which) -> {
                // Get the multi-choice list view
                ListView listView = ((AlertDialog) dialog).getListView();
                SparseBooleanArray checked = listView.getCheckedItemPositions();

                // Clear current POIs and add newly selected ones
                list.getPoiIds().clear();
                for (int i = 0; i < allPOIs.size(); i++) {
                    if (checked.get(i)) {
                        list.addPOI(allPOIs.get(i).getId());
                    }
                }
                localStorage.updatePOIList(list);
                Toast.makeText(this, "POIs added to list", Toast.LENGTH_SHORT).show();
            });

            builder.setNegativeButton("Cancel", null);
        }

        builder.show();
    }

    /**
     *  Listens for location changes and checks for nearby POIs
     */
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    checkNearbySavedPOIs(userLatLng);
                }
            }
        };
    }

    /**
     * Compares user location and POI location for distance
     */
    private void checkNearbySavedPOIs(LatLng userLatLng) {
        List<POI> nearbyPOIs = new ArrayList<>();
        DistanceCalculator haversine = (lat1, lon1, lat2, lon2) -> {
            double R = 6371;
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        };

        Log.d("PROXIMITY", "User location: " + userLatLng);
        for (POI poi : allPOIs) {
            double distance = haversine.calculate(
                    userLatLng.latitude, userLatLng.longitude,
                    poi.getLocation().latitude, poi.getLocation().longitude
            );
            Log.d("PROXIMITY", "POI: " + poi.getName() + " | Distance: " + distance + " km");
            if (distance * 1000 <= PROXIMITY_RADIUS_METERS) { // Convert km to meters
                nearbyPOIs.add(poi);
                Log.d("PROXIMITY", "Triggering notification for: " + poi.getName());
            }
        }

        if (!nearbyPOIs.isEmpty()) {
            showProximityNotification(nearbyPOIs);
        }
    }

    /**
     *  Method to display alerts
     */
    private void showProximityNotification(List<POI> nearbyPOIs) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime < NOTIFICATION_COOLDOWN_MS) {
            Log.d("NOTIFICATION", "Skipping notification - too soon since last one");
            return;
        }
        lastNotificationTime = currentTime;

        if (nearbyPOIs.isEmpty()) return;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Create a more informative notification
        String contentTitle;
        String contentText;

        if (nearbyPOIs.size() == 1) {
            POI poi = nearbyPOIs.get(0);
            contentTitle = "Nearby: " + poi.getName();
            contentText = poi.getDisplayInfo() + " is nearby";
        } else {
            contentTitle = nearbyPOIs.size() + " POIs nearby!";
            StringBuilder sb = new StringBuilder();
            for (POI poi : nearbyPOIs) {
                sb.append("• ").append(poi.getName()).append("\n");
            }
            contentText = sb.toString().trim();
        }

        // Create a notification channel (already done in createNotificationChannel())

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "proximity_alerts")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            manager.notify(1, builder.build());
            Log.d("NOTIFICATION", " + nearbyPOIs.size() + ");
        } catch (Exception e) {
            Log.e("NOTIFICATION", "Error: " + e.getMessage());
        }
    }

    /**
     *  Shows Dialog for custom list POI
     */
    private void showAddToListDialog(POI poi) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add " + poi.getName() + " to list");

        // Filter out null items (the "Show All" option)
        List<POIList> filterableLists  = new ArrayList<>();
        for (POIList list : poiLists) {
            if (!list.isShowAll()) {
                filterableLists.add(list);
            }
        }

        if (filterableLists .isEmpty()) {
            builder.setMessage("No lists available. Create one first.");
            builder.setPositiveButton("OK", null);
        } else {
            String[] listNames = new String[filterableLists .size()];
            boolean[] checkedItems = new boolean[filterableLists .size()];

            for (int i = 0; i < filterableLists .size(); i++) {
                listNames[i] = filterableLists .get(i).getName();
                checkedItems[i] = filterableLists .get(i).getPoiIds().contains(poi.getId());
            }

            builder.setMultiChoiceItems(listNames, checkedItems, (dialog, which, isChecked) -> {
                POIList list = filterableLists .get(which);
                if (isChecked) {
                    list.addPOI(poi.getId());
                } else {
                    list.removePOI(poi.getId());
                }
                localStorage.updatePOIList(list);
            });

            builder.setPositiveButton("Done", null);
        }

        builder.show();
    }

    /**
     * Shows dialog for real-time address search with suggestions using Places Text Search (New API)
     */
    private void showAddressInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search for Address");

        // Create layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        // AutoCompleteTextView for real-time search
        AutoCompleteTextView searchInput = new AutoCompleteTextView(this);
        searchInput.setHint("Start typing an address...");
        searchInput.setThreshold(3); // Start searching after 3 characters
        layout.addView(searchInput);

        // ListView to show suggestions
        ListView suggestionsList = new ListView(this);
        suggestionsList.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400 // height for suggestions
        ));
        layout.addView(suggestionsList);

        builder.setView(layout);

        // Create adapter for suggestions
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        suggestionsList.setAdapter(adapter);

        // Store returned Place objects
        List<Place> currentPlaces = new ArrayList<>();

        // Set up real-time search
        Handler searchHandler = new Handler(Looper.getMainLooper());
        final Runnable[] searchRunnable = {null};

        // Search input listener, handles real time typing
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable[0] != null) {
                    searchHandler.removeCallbacks(searchRunnable[0]);
                }

                String query = s.toString().trim();
                if (query.length() >= 3) {
                    // Delay search by 300ms to avoid too many API calls
                    Runnable newSearchRunnable = () -> searchAddresses(query, adapter, currentPlaces);
                    searchHandler.postDelayed(newSearchRunnable, 300);
                    searchRunnable[0] = newSearchRunnable;
                } else {
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    currentPlaces.clear();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Create the dialog first so we can dismiss it in the click listener
        AlertDialog dialog = builder.setNegativeButton("Cancel", (d, which) -> {
            toggleAddressInputMode();
            d.cancel();
        }).create();

        // Handle poi address selection
        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            try {
                Log.d("PLACE_CLICK", "Item clicked at position: " + position + ", currentPlaces size: " + currentPlaces.size());

                // Gets dynamic position of user to bias search results
                if (position >= 0 && position < currentPlaces.size()) {
                    Place selectedPlace = currentPlaces.get(position);
                    Log.d("PLACE_CLICK", "Selected place: " + (selectedPlace != null ? selectedPlace.getDisplayName() : "null"));

                    if (selectedPlace != null && selectedPlace.getLocation() != null) {
                        // Move camera to location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getLocation(), 15));

                        // Show POI creation dialog with place name as default
                        String placeName = selectedPlace.getDisplayName() != null ?
                                selectedPlace.getDisplayName() : "New Location";

                        // Dismiss the search dialog first
                        dialog.dismiss();

                        // CREATES NEW POI (cals method) BASED ON THE SELECTED NAME AND FETCHES LOCATION
                        showPOICreationDialog(selectedPlace.getLocation(), placeName);

                        Toast.makeText(this, "Location found: " + placeName, Toast.LENGTH_SHORT).show();

                        // Reset address input mode
                        toggleAddressInputMode();
                    } else {
                        Log.e("PLACE_CLICK", "Selected place is null or has no location data");
                        Toast.makeText(this, "Location data not available for this place", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("PLACE_CLICK", "Invalid position: " + position + " for size: " + currentPlaces.size());
                    Toast.makeText(this, "Invalid selection", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("PLACE_CLICK", "Error handling place selection", e);
                Toast.makeText(this, "Error selecting place: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /**
     * Search for address suggestions using Places Autocomplete API
     */
    private void searchAddresses(String query, ArrayAdapter<String> adapter, List<Place> currentPlaces) {
        // Define the fields you want (required!)
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,  // Use DISPLAY_NAME instead of NAME
                Place.Field.LOCATION //latlng
        );

        // Build the request - using dynamic location bias based on user location
        // Use the builder to create a SearchByTextRequest object.
        SearchByTextRequest.Builder requestBuilder = SearchByTextRequest.builder(query, placeFields)
                .setMaxResultCount(5);

        // Add location bias if user location is available
        if (userLocation != null) {
            // Create a circular bias around user location (20km radius)
            CircularBounds circularBias = CircularBounds.newInstance(userLocation, 20000.0); // 20km radius
            requestBuilder.setLocationBias(circularBias);
            Log.d("SEARCH", "Using location bias around user location: " + userLocation.toString());
        } else {
            Log.d("SEARCH", "No user location available, using IP-based bias");
            // If no user location, Google will use IP-based bias automatically
        }

        SearchByTextRequest searchByTextRequest = requestBuilder.build();

        // Perform search
        placesClient.searchByText(searchByTextRequest)
                .addOnSuccessListener(response -> {
                    List<Place> places = response.getPlaces(); // gets places objects as list

                    // Clear the existing places
                    adapter.clear();
                    currentPlaces.clear();

                    // For each place found, get its name and add the place object to currentPlaces
                    for (Place place : places) {
                        if (place.getDisplayName() != null) {
                            adapter.add(place.getDisplayName());
                            currentPlaces.add(place); // Add the Place object to currentPlaces
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Places", "Search by text failed: " + e.getMessage());
                    Toast.makeText(this, "Search failed: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Shows POI creation dialog with just location (for map clicks)
     */
    private void showPOICreationDialog(LatLng location) {
        showPOICreationDialog(location, ""); // Call the new version with empty name
    }

    /**
     * Overload, Shows dialog for creating a new POI at the specified address
     */
    private void showPOICreationDialog(LatLng location, String defaultName) {
        // Check if there's already a POI at this location
        POI existingPOI = findPOIAtLocation(location);
        boolean isEditing = existingPOI != null;

        // Create dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        // POI Name input with default value
        EditText nameInput = new EditText(this);
        nameInput.setText(defaultName != null ? defaultName : "");
        nameInput.setHint("Enter POI name");
        layout.addView(nameInput);

        // POI Type spinner
        Spinner typeSpinner = new Spinner(this);
        String[] poiTypes = {"Restaurant", "Historical Site", "Park"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, poiTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        // Set selected type if editing
        if (isEditing) {
            if (existingPOI instanceof RestaurantPOI) {
                typeSpinner.setSelection(0);
            } else if (existingPOI instanceof HistoricalPOI) {
                typeSpinner.setSelection(1);
            } else if (existingPOI instanceof ParkPOI) {
                typeSpinner.setSelection(2);
            }
        }
        layout.addView(typeSpinner);

        // Additional info input (cuisine type, description, etc.)
        EditText additionalInfoInput = new EditText(this);
        additionalInfoInput.setHint("Additional info (cuisine type, description, etc.)");
        if (isEditing) {
            if (existingPOI instanceof RestaurantPOI) {
                additionalInfoInput.setText(((RestaurantPOI) existingPOI).getCuisineType());
            } else if (existingPOI instanceof HistoricalPOI) {
                additionalInfoInput.setText(((HistoricalPOI) existingPOI).getDescription());
            } else if (existingPOI instanceof ParkPOI) {
                additionalInfoInput.setText(((ParkPOI) existingPOI).getDescription());
            }
        }
        layout.addView(additionalInfoInput);

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEditing ? "Edit POI" : "Create New POI")
                .setView(layout)
                .setPositiveButton(isEditing ? "Update" : "Create", (dialog, which) -> { //no update feature implemented yet
                    String name = nameInput.getText().toString().trim();
                    String additionalInfo = additionalInfoInput.getText().toString().trim();
                    String selectedType = typeSpinner.getSelectedItem().toString();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter a POI name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (additionalInfo.isEmpty()) {
                        additionalInfo = getDefaultAdditionalInfo(selectedType);
                    }

                    // Create new POI
                    createNewPOI(name, location, selectedType, additionalInfo);

                    // Reset adding modes
                    resetPOIAddingMode();
                    if (isAddingByAddress) {
                        isAddingByAddress = false;
                        addPOIByAddressButton.setText("Add POI by Address");
                        addPOIByAddressButton.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Reset adding modes on cancel
                    resetPOIAddingMode();
                    if (isAddingByAddress) {
                        isAddingByAddress = false;
                        addPOIByAddressButton.setText("Add POI by Address");
                        addPOIByAddressButton.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
                    }
                });
        builder.setCancelable(false) // Prevent accidental cancellation
                .show();
    }

    /**
     * Creates a new POI based on user input
     */
    private void createNewPOI(String name, LatLng location, String type, String additionalInfo) {
        POI newPOI;
        String poiId = UUID.randomUUID().toString(); // Crate random ID for the POI

        // Debug log
        System.out.println("Creating POI: " + name + " at " + location.latitude + ", " + location.longitude);

        switch (type) {
            case "Restaurant":
                newPOI = new RestaurantPOI(name, location, additionalInfo);
                break;
            case "Historical Site":
                newPOI = new HistoricalPOI(name, location, additionalInfo);
                break;
            case "Park":
                newPOI = new ParkPOI(name, location, additionalInfo);
                break;
            default:
                newPOI = new POI(name, location);
                break;
        }

        // Set the ID for the POI
        newPOI.id = poiId;

        // Add the POI
        addUserPOI(newPOI);

        // Debug log
        System.out.println("POI created with ID: " + newPOI.getId());
    }

    /**
     * Update existing POI, not implemented
     */
    private void updateExistingPOI(POI existingPOI, String name, LatLng location, String type, String additionalInfo) {
        // Remove old POI
        deleteUserPOI(existingPOI);

        // Create new POI with same ID
        POI updatedPOI;
        String poiId = existingPOI.getId();

        switch (type) {
            case "Restaurant":
                updatedPOI = new RestaurantPOI(name, location, additionalInfo);
                break;
            case "Historical Site":
                updatedPOI = new HistoricalPOI(name, location, additionalInfo);
                break;
            case "Park":
                updatedPOI = new ParkPOI(name, location, additionalInfo);
                break;
            default:
                updatedPOI = new POI(name, location);
                break;
        }

        // Set the same ID
        updatedPOI.id = poiId;

        // Add the updated POI
        addUserPOI(updatedPOI);

        Toast.makeText(this, "POI updated successfully!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper method to reset POI adding mode
     */
    private void resetPOIAddingMode() {
        if (isAddingPOI) {
            isAddingPOI = false;
            addPOIButton.setText("Add POI");
            addPOIButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
        }
    }

    /**
     * Helper method to search POI by ID
     */
    private POI findPOIById(String id) {
        for (POI poi : allPOIs) {
            if (poi.getId().equals(id)) {
                return poi;
            }
        }
        return null;
    }

    /**
     * Helper method to find POI
     */
    private POI findPOIAtLocation(LatLng location) {
        final double TOLERANCE = 0.0001; // Small tolerance for location comparison

        for (POI poi : allPOIs) {
            if (Math.abs(poi.getLocation().latitude - location.latitude) < TOLERANCE &&
                    Math.abs(poi.getLocation().longitude - location.longitude) < TOLERANCE) {
                return poi;
            }
        }
        return null;
    }

    /**
     * Helper method that provides default additional info based on POI type
     */
    private String getDefaultAdditionalInfo(String type) {
        switch (type) {
            case "Restaurant":
                return "Unknown Cuisine";
            case "Historical Site":
                return "Historical Site";
            case "Park":
                return "Public Park";
            default:
                return "Point of Interest";
        }
    }

    /**
     * Populates the Points of Interest (POI) map with predefined locations.
     *
     * This method adds various types of POIs to the map, including:
     * - Restaurants (with cuisine types)
     * - Historical sites
     * - Parks
     *
     * Each POI is added using the x method.
     */
    private void initializePOIMap() {

        allPOIs.clear();

        // Add user-generated POIs from local storage
        List<POI> userPOIs = localStorage.loadUserPOIs();
        allPOIs.addAll(userPOIs);

        // Add all POIs to map
        for (POI poi : allPOIs) {
            addPOIToMap(poi);
        }
    }

    /**
     * Adds a Point of Interest (POI) to the map and the POI collection.
     *
     * This method:
     * - Adds a marker to the Google Map for the POI
     * - Stores the POI in the {@code poiMap} for nearest neighbor calculations
     *
     * @param poi The Point of Interest to be added
     */
    private void addPOIToMap(POI poi) {
        try {
            // Create marker with different colors for different POI types
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(poi.getLocation())
                    .title(poi.getName())
                    .snippet(poi.getDisplayInfo());

            // Set different marker colors for different POI types
            if (poi instanceof RestaurantPOI) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            } else if (poi instanceof HistoricalPOI) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            } else if (poi instanceof ParkPOI) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }

            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                poiMarkers.put(poi.getId(), marker);

                // Also add to poiMap for KNN algorithm
                poiMap.put(poi.getName(), poi.getLocation());

                System.out.println("Marker added to map for POI: " + poi.getName());
            } else {
                System.out.println("Failed to add marker to map for POI: " + poi.getName());
            }

        } catch (Exception e) {
            System.out.println("Error adding POI to map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds user POI to memory
     */
    public void addUserPOI(POI newPOI) {
        // Add to our collections
        allPOIs.add(newPOI);

        // Save to local storage
        localStorage.addPOI(newPOI);

        // Add to map
        addPOIToMap(newPOI);

        Toast.makeText(this, "POI '" + newPOI.getName() + "' saved successfully!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Deletes user POI from memory
     */
    public void deleteUserPOI(POI poi) {
        // Remove from collections
        allPOIs.remove(poi);
        poiMap.remove(poi.getName());

        // Remove from local storage
        localStorage.removePOI(poi.getId());

        // Remove marker from map
        Marker marker = poiMarkers.get(poi.getId());
        if (marker != null) {
            marker.remove();
            poiMarkers.remove(poi.getId());
        }

        Toast.makeText(this, "POI deleted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Finds and displays the nearest Points of Interest to the user's location.
     *
     * This method:
     * - Retrieves the 5 nearest POIs within 5 km
     * - Adds markers for these POIs on the map
     * - Displays an AlertDialog with the names of the nearest POIs
     */
    private void findNearestPOIsAndDisplay(){
        // Create List called nearestPOIs and retrieve the nearest 5 POIs with method findNearestPOIs
        List<POI> nearestPOIs = findNearestPOIs(userLocation.latitude, userLocation.longitude, 5);

        // Add markers for each of the nearest POIs
        for (POI poi : nearestPOIs) { // for each entry in the list nearestPOIs
            mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.dot_icon))
                    .position(poi.getLocation())
                    .title(poi.getName()));
        }

        String[] poiNames = nearestPOIs.stream() // Creates stream from the nearestPOIs List
                .map(POI::getName) // Uses map to transform each POI into its name with the getter
                .toArray(String[]::new); // Saves the stream of names into a String array

        // Create and display poiNames
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Nearest POIs")
                .setItems(poiNames, null)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Works with Distance calculator
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2, DistanceCalculator haversine) {
        return haversine.calculate(lat1, lon1, lat2, lon2);
    }

    /**
     * Finds the nearest Points of Interest (POIs) to a given user location.
     *
     * This method:
     * - Calculates distances between the user's location and all available POIs
     * - Filters POIs within a maximum distance of 5 km
     * - Sorts the POIs by their proximity to the user's location
     *
     * @param userLat Latitude of the user's current location
     * @param userLng Longitude of the user's current location
     * @param k Maximum number of nearest POIs to return (not used in current implementation)
     * @return A list of POIs sorted by their distance from the user, within 5 km
     *
     * @see DistanceCalculator Haversine distance calculation interface
     */
    public List<POI> findNearestPOIs(double userLat, double userLng, int k) {
        List<POI> nearbyPOIs = new ArrayList<>();
        final double maxDistanceKm = 5.0;

        DistanceCalculator haversine = (lat1, lon1, lat2, lon2) -> {
            double R = 6371;
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        };

        // Use allPOIs
        for (POI poi : allPOIs) {
            double distance = calculateDistance(userLat, userLng,
                    poi.getLocation().latitude, poi.getLocation().longitude, haversine);

            if (distance <= maxDistanceKm) {
                nearbyPOIs.add(poi);
            }
        }

        // Sort by distance
        nearbyPOIs.sort(Comparator.comparingDouble(poi ->
                calculateDistance(userLat, userLng,
                        poi.getLocation().latitude, poi.getLocation().longitude, haversine)));

        return nearbyPOIs.size() > k ? nearbyPOIs.subList(0, k) : nearbyPOIs;
    }

    /**
     * Functional interface for calculating distances between geographical coordinates.
     *
     * Uses the Haversine formula to compute great-circle distances between two points.
     */
    @FunctionalInterface
    public interface DistanceCalculator {
        /**
         * Calculates the distance between two points on Earth.
         *
         * @param lat1 Latitude of the first point
         * @param lon1 Longitude of the first point
         * @param lat2 Latitude of the second point
         * @param lon2 Longitude of the second point
         * @return Distance between the points in kilometers
         */
        double calculate(double lat1, double lon1, double lat2, double lon2);
    }

    // Export user POIs to JSON file
    public void exportUserPOIs() {
        List<POI> userPOIs = localStorage.loadUserPOIs();

        if (userPOIs.isEmpty()) {
            Toast.makeText(this, "No user POIs to export", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (POI poi : userPOIs) {
            JSONObject json = poi.toJSON();
            if (json != null) {
                jsonArray.put(json);
            }
        }

        // Save to external storage or share
        try {
            String fileName = "my_pois_" + System.currentTimeMillis() + ".json";
            File file = new File(getExternalFilesDir(null), fileName);

            FileWriter writer = new FileWriter(file);
            writer.write(jsonArray.toString(2)); // Pretty print
            writer.close();

            Toast.makeText(this, "POIs exported to " + file.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Represents a generic Point of Interest with a name and location.
     *
     * This base class serves as a foundation for more specific POI types.
     * Subclasses include RestaurantPOI, HistoricalPOI, and ParkPOI.
     */
    public static class POI {
        // Attributes
        String name;
        LatLng location;
        String id;

        // Constructor
        public POI(String name, LatLng location) {
            this.name = name;
            this.location = location;
            this.id = UUID.randomUUID().toString();;
        }

        // getters and setters
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public LatLng getLocation() {
            return location;
        }
        public void setLocation(LatLng location) {
            this.location = location;
        }
        public String getId(){ return id; }
        public String getPOIType(){
            return "Simple POI";
        };
        public String getDisplayInfo() {
            // Get lists this POI belongs to
            List<String> listNames = new ArrayList<>();
            for (POIList list : poiLists) {
                if (list.getPoiIds().contains(id)) {
                    listNames.add(list.getName());
                }
            }

            String listInfo = listNames.isEmpty() ? "" :
                    "\nIn lists: " + String.join(", ", listNames);

            return name + " (" + getPOIType() + ")" + listInfo;
        }


        //Method that converts POI to JSON for local storage
        public JSONObject toJSON() {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("name", name);
                json.put("latitude", location.latitude);
                json.put("longitude", location.longitude);
                json.put("poiType", getPOIType());
                return json;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        //Method to Create POI from JSON
        public static POI fromJSON(JSONObject json){
            try {
                String id = json.getString("id");
                String name = json.getString("name");
                double lat = json.getDouble("latitude");
                double lng = json.getDouble("longitude");

                LatLng location = new LatLng(lat, lng);
                POI poi = new POI(name, location);
                poi.id = id;
                return poi;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Represents a restaurant Point of Interest with additional cuisine type information.
     * Extends the base POI class to include specific restaurant details.
     */
    public static class RestaurantPOI extends POI {
        // Attribute
        private String cuisineType;

        // Constructor
        public RestaurantPOI(String name, LatLng location, String cuisineType) {
            super(name, location);
            this.cuisineType = cuisineType;
        }

        // getters and setters
        public String getCuisineType() {
            return cuisineType;
        }
        public void setCuisineType(String cuisineType) {
            this.cuisineType = cuisineType;
        }

        @Override
        public String getPOIType() {
            return cuisineType;
        }

        // Create JSON restaurant data and override for restaurant specific errors
        @Override
        public JSONObject toJSON(){
            JSONObject json = super.toJSON();
            try {
                if (json != null) {
                    json.put("cuisineType", cuisineType);
                    json.put("className", "RestaurantPOI");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        // Method to create restaurant POI from JSON data
        public static RestaurantPOI fromJSON(JSONObject json) {
            try {
                String name = json.getString("name");
                double lat = json.getDouble("latitude");
                double lng = json.getDouble("longitude");
                String cuisineType = json.optString("cuisineType", "Unknown");

                LatLng location = new LatLng(lat, lng);
                RestaurantPOI poi = new RestaurantPOI(name, location, cuisineType);
                poi.id = json.getString("id");
                return poi;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Represents a historical Point of Interest with a descriptive attribute.
     * Extends the base POI class to include historical site details.
     */
    public static class HistoricalPOI extends POI{
        // Attribute
        private String description;

        // Constructor
        public HistoricalPOI(String name, LatLng location, String description){
            super(name, location);
            this.description = description;
        }

        // getters and setters
        public String getDescription(){ return description; }
        public void setDescription(String Description){ this.description = description; }

        @Override
        public String getPOIType(){
            return description;
        }

        // Create JSON historicalpoi data and override for historicalpoi specific errors
        @Override
        public JSONObject toJSON() {
            JSONObject json = super.toJSON();
            try {
                if (json != null) {
                    json.put("description", description);
                    json.put("className", "HistoricalPOI");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        // Method to create historicalpoi POI from JSON data
        public static HistoricalPOI fromJSON(JSONObject json) {
            try {
                String name = json.getString("name");
                double lat = json.getDouble("latitude");
                double lng = json.getDouble("longitude");
                String description = json.optString("description", "Historical Site");

                LatLng location = new LatLng(lat, lng);
                HistoricalPOI poi = new HistoricalPOI(name, location, description);
                poi.id = json.getString("id");
                return poi;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Represents a park Point of Interest with a descriptive attribute.
     * Extends the base POI class to include park-specific details.
     */
    public static class ParkPOI extends POI{
        // Attribute
        private String description;

        // Constructor
        public ParkPOI(String name, LatLng location, String description){
            super(name, location);
            this.description = description;
        }

        // getters and setters
        public String getDescription(){ return description; }
        public void setDescription(String Description){ this.description = description; }

        @Override
        public String getPOIType(){
            return description;
        }

        // Create JSON parkpoi data and override for parkpoi specific errors
        @Override
        public JSONObject toJSON() {
            JSONObject json = super.toJSON();
            try {
                if (json != null) {
                    json.put("description", description);
                    json.put("className", "ParkPOI");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        // Method to create park POI from JSON data
        public static ParkPOI fromJSON(JSONObject json) {
            try {
                String name = json.getString("name");
                double lat = json.getDouble("latitude");
                double lng = json.getDouble("longitude");
                String description = json.optString("description", "Park");

                LatLng location = new LatLng(lat, lng);
                ParkPOI poi = new ParkPOI(name, location, description);
                poi.id = json.getString("id");
                return poi;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * NEW, DYNAMIC STORAGE AND POI DATA 5/22/2025
     */
    public class POILocalStorage {
        static final String PREF_NAME = "poi_storage";
        static final String KEY_USER_POIS = "user_pois";
        static final String KEY_POI_LISTS = "poi_lists";
        SharedPreferences sharedPreferences;
        Context context;

        public POILocalStorage(Context context) {
            this.context = context;
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        // Save user POIs to SharedPreferences
        public void saveUserPOIs(List<POI> userPOIs) {
            JSONArray jsonArray = new JSONArray(); // Save JSON as JSONArray

            // Convert each POI to JSON and add to the array
            for (POI poi : userPOIs) {
                JSONObject json = poi.toJSON();
                if (json != null) {
                    jsonArray.put(json);
                }
            }

            sharedPreferences.edit() // Edit memory
                    .putString(KEY_USER_POIS, jsonArray.toString()) // Saving data
                    .apply(); // Save changes
        }

        // Load user POIs from SharedPreferences
        public List<POI> loadUserPOIs() {
            List<POI> userPOIs = new ArrayList<>(); //Create userPOIs Array to pull JSONArray
            String jsonString = sharedPreferences.getString(KEY_USER_POIS, "[]"); // Pull data from memory

            try {
                JSONArray jsonArray = new JSONArray(jsonString);

                for (int i = 0; i < jsonArray.length(); i++) { // Iterate over saved data list
                    JSONObject json = jsonArray.getJSONObject(i); // Pull saved data
                    POI poi = createPOIFromJSON(json); // Convert data to POI object
                    if (poi != null) {
                        userPOIs.add(poi);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return userPOIs;
        }

        // Helper method to create appropriate POI subclass from JSON
        public POI createPOIFromJSON(JSONObject json) {
            try {
                String className = json.optString("className", "POI");

                switch (className) {
                    case "RestaurantPOI":
                        return RestaurantPOI.fromJSON(json);
                    case "HistoricalPOI":
                        return HistoricalPOI.fromJSON(json);
                    case "ParkPOI":
                        return ParkPOI.fromJSON(json);
                    default:
                        return POI.fromJSON(json);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Add single POI
        public void addPOI(POI poi) {
            List<POI> userPOIs = loadUserPOIs();
            userPOIs.add(poi);
            saveUserPOIs(userPOIs);

        }

        // Remove POI by ID
        public void removePOI(String poiId) {
            List<POI> userPOIs = loadUserPOIs();
            userPOIs.removeIf(poi -> poi.getId().equals(poiId));
            saveUserPOIs(userPOIs);
        }

        // Update existing POI
        public void updatePOI(POI updatedPOI) {
            List<POI> userPOIs = loadUserPOIs();

            for (int i = 0; i < userPOIs.size(); i++) {
                if (userPOIs.get(i).getId().equals(updatedPOI.getId())) {
                    userPOIs.set(i, updatedPOI);
                    break;
                }
            }

            saveUserPOIs(userPOIs);
        }

        // Get total count of user POIs
        public int getUserPOICount() {
            return loadUserPOIs().size();
        }

        // Clear all user POIs (for reset functionality)
        public void clearAllUserPOIs() {
            sharedPreferences.edit().remove(KEY_USER_POIS).apply();
        }

        // Save POI Custom Lists
        public void savePOILists(List<POIList> lists) {
            JSONArray jsonArray = new JSONArray();
            for (POIList list : lists) {
                jsonArray.put(list.toJSON());
            }
            sharedPreferences.edit()
                    .putString(KEY_POI_LISTS, jsonArray.toString())
                    .apply();
        }

        // Load POI Custom Lists
        public List<POIList> loadPOILists() {
            List<POIList> lists = new ArrayList<>();
            String jsonString = sharedPreferences.getString(KEY_POI_LISTS, "[]");

            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    POIList list = POIList.fromJSON(jsonArray.getJSONObject(i));
                    if (list != null) {
                        lists.add(list);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return lists;
        }

        // Add POI Custom Lists
        public void addPOIList(POIList list) {
            List<POIList> lists = loadPOILists();
            lists.add(list);
            savePOILists(lists);
        }

        // Delete POI Custom Lists
        public void removePOIList(String listId) {
            List<POIList> lists = loadPOILists();
            lists.removeIf(list -> list.getId().equals(listId));
            savePOILists(lists);
        }

        // Update POI Custom Lists
        public void updatePOIList(POIList updatedList) {
            List<POIList> lists = loadPOILists();
            for (int i = 0; i < lists.size(); i++) {
                if (lists.get(i).getId().equals(updatedList.getId())) {
                    lists.set(i, updatedList);
                    break;
                }
            }
            savePOILists(lists);
        }
    }

    /**
     *  POI Custom Lists 5/23/2025
     */
    public static class POIList {
        private String id;
        private String name;
        private String description;
        private List<String> poiIds; // Stores IDs of POIs in this list
        private boolean isShowAll;

        public POIList(String name, String description) {
            this(name, description, false);
        }

        // New constructor for special lists
        public POIList(String name, String description, boolean isShowAll) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.description = description;
            this.poiIds = new ArrayList<>();
            this.isShowAll = isShowAll;
        }

        // Getters and setters
        public String getId() { return id; }
        public String getName() { return this.name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getPoiIds() { return poiIds; }
        public boolean isShowAll() { return isShowAll; }
        @Override
        public String toString() {
            return name; // This ensures default adapter behavior uses getName()
        }

        // Add POI to list
        public void addPOI(String poiId) {
            if (!poiIds.contains(poiId)) {
                poiIds.add(poiId);
            }
        }

        // Remove POI from list
        public void removePOI(String poiId) {
            poiIds.remove(poiId);
        }

        // Convert to JSON
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("name", name);
                json.put("description", description);

                JSONArray poiArray = new JSONArray();
                for (String poiId : poiIds) {
                    poiArray.put(poiId);
                }
                json.put("poiIds", poiArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        // Create from JSON
        public static POIList fromJSON(JSONObject json) {
            try {
                String name = json.getString("name");
                String description = json.getString("description");
                POIList list = new POIList(name, description);
                list.id = json.getString("id");

                JSONArray poiArray = json.getJSONArray("poiIds");
                for (int i = 0; i < poiArray.length(); i++) {
                    list.poiIds.add(poiArray.getString(i));
                }
                return list;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    class POIListSpinnerAdapter extends ArrayAdapter<POIList> {
        public POIListSpinnerAdapter(Context context, List<POIList> lists) {
            super(context, android.R.layout.simple_spinner_item, lists);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setText(getItem(position).getName());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setText(getItem(position).getName());
            return view;
        }
    }

}