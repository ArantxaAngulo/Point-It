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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
//import com.google.android.libraries.places.api.net.NearbySearchRequest;
import android.location.Location;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.model.LatLng;
import android.location.Location;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import android.location.Location;
import android.util.Log;
import android.widget.Button;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

/*
- Classes ✔️
- Objects ✔️
- Constructors ✔️
- this keyword ✔️
- access modifiers ✔️
- getters and setters ️✔️
- multiple levels of inheritance ✔️
- collections framework ✔️
- lambda ✔️
- stream ✔️
- enum types ❓
- interfaces ✔️
 */

// "MAIN" CLASS
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // New Object GoogleMap
    private FusedLocationProviderClient fusedLocationClient; // Object FusedLocationProviderClient
    Map<String, LatLng> poiMap = new HashMap<>(); // HashMap named poiMap that relates PLACENAME String to COORDINATES LatLng // LatLng Datatype provided by google
    private LatLng userLocation; // Coordinates to store user location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize the button
        Button findNearestButton = findViewById(R.id.find_nearest_button); // Variable "findNearestButton" of type Button is equal to ID find_nearest_button
        findNearestButton.setOnClickListener(v -> findNearestPOIsAndDisplay()); // Whenever the button is clicked, view resets


        // Initializing client location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap; // GoogleMap Object

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permissions if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        // Enable "My Location" button
        mMap.setMyLocationEnabled(true);

        // Get the last known location and update the map camera
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {

            @Override
            public void onSuccess(Location location) { // Method onSuccess that rececives location
                if (location != null) { // While the location is given...

                    userLocation = new LatLng(location.getLatitude(), location.getLongitude()); // Convert location to LatLng Object and save it to userLocation
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15)); // Move the camera to userLocation

                    // DATA
                    initializePOIMap();

                }
            }
        });
    }

    private void initializePOIMap() {

        // Add POIs using a helper method
        addPOI(new RestaurantPOI("El Hueco", new LatLng(20.6049088, -103.4153376), "Bar"));
        addPOI(new RestaurantPOI("Tacos El Ojitos", new LatLng(20.6083417, -103.4326965), "Tacos"));
        addPOI(new RestaurantPOI("Charlie Boy Burgers & Shakes", new LatLng(20.628596, -103.409205), "Hamburguesas"));
        addPOI(new RestaurantPOI("Hamburguesas Beto's", new LatLng(20.641518, -103.420175), "Hamburguesas"));
        addPOI(new RestaurantPOI("Mile Pizzas a la Lena", new LatLng(20.641990, -103.401016), "Pizzas"));
        addPOI(new POI("Casa Macaria Cruz del Sur", new LatLng(20.642652, -103.387851)));
        addPOI(new RestaurantPOI("Pizzas D'TERE", new LatLng(20.650405, -103.432099), "Pizzas"));
        addPOI(new RestaurantPOI("Asador Santa Anita", new LatLng(20.560811, -103.448668), "Carne Asada"));
        addPOI(new RestaurantPOI("Danko Deep Dish", new LatLng(20.637636, -103.311561), "Pizza"));
        addPOI(new RestaurantPOI("Muchang", new LatLng(20.65440121259779, -103.40111706108912), "Oriental"));
        addPOI(new RestaurantPOI("Karnitas La Terraza", new LatLng(20.67235947117652, -103.43024405924054), "Tacos"));
        addPOI(new RestaurantPOI("Benitos Pizza & Pasta", new LatLng(20.642920136197883, -103.41698452809095), "Pizza"));
        addPOI(new RestaurantPOI("Archipielago Pizza", new LatLng(20.676630936456995, -103.36731186108844), "Pizza"));
        addPOI(new RestaurantPOI("Super Taco Tomas", new LatLng(20.673799864260175, -103.38631943225309), "Tacos"));
        addPOI(new RestaurantPOI("Ragazzi Pizzeria", new LatLng(20.677997908494408, -103.37250427643016), "Pizza"));
        addPOI(new RestaurantPOI("Tripitas Tacos Gdl", new LatLng(20.694243134278665, -103.3270260524139), "Tacos"));
        addPOI(new RestaurantPOI("Algún Lugar Pizzería", new LatLng(20.660623118786983, -103.40551649177243), "Pizza"));
        addPOI(new RestaurantPOI("La Churrasquería By Maneiro", new LatLng(20.682163472904335, -103.38976951757914), "Buffet"));
        addPOI(new RestaurantPOI("Carlos", new LatLng(20.657948383652645, -103.36983603019439), "Tacos"));
        addPOI(new RestaurantPOI("Antojitos Mexicanos Lulú", new LatLng(20.654686674361386, -103.4506738087742), "Mexicana"));
        addPOI(new RestaurantPOI("Casa Ledezma", new LatLng(20.641011442070898, -103.3122143764312), "Mexicana"));
        addPOI(new RestaurantPOI("Carnitas Vale", new LatLng(20.742718615700394, -103.40664526293457), "Tacos"));
        addPOI(new RestaurantPOI("LOS ROJOS DE TJ", new LatLng(20.677464222168737, -103.43642208992385), "Tacos"));
        addPOI(new RestaurantPOI("NASSH", new LatLng(20.65905555970178, -103.33620579177244), "Tortas"));
        addPOI(new RestaurantPOI("¿Dónde María?", new LatLng(20.655682724991717, -103.31798639177258), "Tacos"));
        addPOI(new RestaurantPOI("Yogibear", new LatLng(20.665643928013576, -103.33248753040529), "Pizza"));
        addPOI(new RestaurantPOI("Suki Sushi Buffet", new LatLng(20.675020735479443, -103.35100544759479), "Sushi"));
        addPOI(new RestaurantPOI("El Güero Fermín", new LatLng(20.663094015683196, -103.4309936745826), "Tacos"));
        addPOI(new RestaurantPOI("Tripitas Maribel", new LatLng(20.675379238113024, -103.33939753410117), "Tacos"));
        addPOI(new RestaurantPOI("Tripitas Los Panchos", new LatLng(20.67109177212732, -103.3259117118514), "Tacos"));
        addPOI(new RestaurantPOI("Felipe Zetter", new LatLng(20.633163215882174, -103.43201810952024), "Tacos"));
        addPOI(new RestaurantPOI("El Rojo", new LatLng(20.689461244105246, -103.3593033214712), "Tacos"));
        addPOI(new RestaurantPOI("Tripitas Don Pancho", new LatLng(20.678501627857997, -103.38333623857677), "Tacos"));
        addPOI(new RestaurantPOI("Tripitas Don Ramón", new LatLng(20.70032364996633, -103.35570217827757), "Tacos"));
        addPOI(new RestaurantPOI("Las Carnitas de Don Andrés", new LatLng(20.64484199871797, -103.29164764759565), "Tacos"));
        addPOI(new RestaurantPOI("Gallo Cervecero", new LatLng(20.68191524301125, -103.36411546035171), "Buffet"));
        addPOI(new RestaurantPOI("CORPAR La Casa del Taco", new LatLng(20.674388849767112, -103.3938747610885), "Tacos"));
        addPOI(new RestaurantPOI("Barbacoa Gamero", new LatLng(20.675114983363468, -103.42129643582885), "Tacos"));
        addPOI(new RestaurantPOI("Bocazza Pizza & Chela", new LatLng(20.678943863304948, -103.35322827662436), "Pizza"));
        addPOI(new RestaurantPOI("Chinaloa", new LatLng(20.675020345603308, -103.34767050450789), "Oriental"));
        addPOI(new RestaurantPOI("Mono de Mar", new LatLng(20.675301297387097, -103.41594786583023), "Mariscos"));
        addPOI(new RestaurantPOI("Los de Guadalupe", new LatLng(20.66202511077841, -103.42796760261925), "Tacos"));
        addPOI(new RestaurantPOI("Good Morning Sunshine", new LatLng(20.6686876320358, -103.36561511796026), "Desayuno"));
        addPOI(new RestaurantPOI("Carnes En Su Julio", new LatLng(20.73326932738262, -103.38711640815843), "Tacos"));
        addPOI(new RestaurantPOI("El Machin", new LatLng(20.673501036683515, -103.33373486398341), "Tacos"));
        addPOI(new RestaurantPOI("Ahumaditos", new LatLng(20.699992205527906, -103.32632719466501), "Hamburguesas"));
        addPOI(new RestaurantPOI("Yo Amo La Pizza", new LatLng(20.65888182617726, -103.44269191976154), "Pizza"));
        addPOI(new RestaurantPOI("Tortas Planchadas de Paty Berber", new LatLng(20.669091131582302, -103.4210097007723), "Tortas"));
        addPOI(new RestaurantPOI("Burger Club", new LatLng(20.666419480760368, -103.36236634679551), "Hamburguesas"));
        addPOI(new RestaurantPOI("Capitako", new LatLng(20.731418061402838, -103.4129103333), "Tacos"));
        addPOI(new RestaurantPOI("Acá las tortas", new LatLng(20.678179538066633, -103.37593374469313), "Tortas"));
        addPOI(new RestaurantPOI("Los Guasaveños", new LatLng(20.673434277792573, -103.42691058957773), "HotDog"));
        addPOI(new RestaurantPOI("KeBurros", new LatLng(20.66580089678315, -103.40775879651271), "Burritos"));
        addPOI(new RestaurantPOI("La Casa de Doña Ines", new LatLng(20.699527006144105, -103.37561230077159), "Desayuno"));
        addPOI(new RestaurantPOI("La Ciabatta", new LatLng(20.672393247273774, -103.3574514033342), "Italiana"));
        addPOI(new RestaurantPOI("Los Clásicos", new LatLng(20.629483616971726, -103.42323439168777), "Tacos"));
        addPOI(new RestaurantPOI("Bear&Wolf", new LatLng(20.67306847504227, -103.42851709281867), "Hamburguesas"));
        addPOI(new RestaurantPOI("Mama Burguers", new LatLng(20.654114873537022, -103.42855811426675), "Hamburguesas"));
        addPOI(new RestaurantPOI("Sham Burgers", new LatLng(20.710607253430428, -103.39689681210261), "Hamburguesas"));
        addPOI(new RestaurantPOI("El rincon del asadero", new LatLng(20.664511998146896, -103.39159581611332), "Tacos"));
        addPOI(new RestaurantPOI("Los De Papa", new LatLng(20.633866956330056, -103.42598765650864), "Tacos"));
        addPOI(new RestaurantPOI("Los Mochibrothers", new LatLng(20.663556067292728, -103.4006926946657), "Hot Dog"));
        addPOI(new RestaurantPOI("La Rafaela", new LatLng(20.66223975548912, -103.42273276767746), "Mexicana"));
        addPOI(new RestaurantPOI("Momotabi Mochi Market", new LatLng(20.67614944322779, -103.35700613019515), "Helado"));
        addPOI(new RestaurantPOI("Blacksoul", new LatLng(20.67761619743144, -103.37013798912481), "Desayuno"));
        addPOI(new RestaurantPOI("Bosco Bianco", new LatLng(20.66316600621923, -103.43642403514838), "Helado"));
        addPOI(new RestaurantPOI("Las Grandes Tortas Ahogadas", new LatLng(20.683661840559594, -103.34751598727773), "Tortas"));
        addPOI(new RestaurantPOI("El Chavito", new LatLng(20.68432006536184, -103.37022286398322), "Hot Dog"));
        addPOI(new RestaurantPOI("Yakomi", new LatLng(20.677858834829667, -103.37118456952403), "Oriental"));
        addPOI(new RestaurantPOI("El Terrible Juan", new LatLng(20.66527238068439, -103.39455485980912), "Desayuno"));
        addPOI(new RestaurantPOI("Los Arre", new LatLng(20.629017731012247, -103.39365900503277), "Carne Asada"));
        addPOI(new RestaurantPOI("Templo Bonsai Café", new LatLng(20.676768835929277, -103.36759786898813), "Desayuno"));
        addPOI(new RestaurantPOI("La hamburguesería central", new LatLng(20.673554, -103.365867), "Hamburguesas"));
        addPOI(new RestaurantPOI("EL RINCON DE LALO DE SAHUAYO", new LatLng(20.631301, -103.407347), "Desayuno"));
        addPOI(new RestaurantPOI("Little Caesars", new LatLng(20.611527, -103.416318), "Pizza"));
        addPOI(new RestaurantPOI("Chiles'n verdes", new LatLng(20.634632, -103.392027), "Tacos"));
        addPOI(new RestaurantPOI("Coco Cafe", new LatLng(20.587994, -103.440649), "Desayuno"));
        addPOI(new RestaurantPOI("Campomar Punto Sur", new LatLng(20.569618, -103.454329), "Mariscos"));
        addPOI(new RestaurantPOI("Pizzeria de Barrio", new LatLng(20.677642, -103.36759), "Pizza"));
        addPOI(new RestaurantPOI("Mochitacos", new LatLng(20.717768, -103.455170), "Tacos"));
        addPOI(new RestaurantPOI("Taylor Street Pizza", new LatLng(20.729325, -103.434846), "Pizza"));
        addPOI(new RestaurantPOI("El Rinconcito de las Tortas", new LatLng(20.741844, -103.407740), "Tortas"));
        addPOI(new RestaurantPOI("Bullsnack", new LatLng(20.666536, -103.37853), "Fast Food"));
        addPOI(new RestaurantPOI("Blazz California Burritos", new LatLng(20.706061, -103.416029), "Burritos"));
        addPOI(new RestaurantPOI("Hyper Restaurante Espacial", new LatLng(20.674176, -103.371238), "Fast Food"));
        addPOI(new HistoricalPOI("Biblioteca ITESO", new LatLng(20.606140222513815, -103.41562983271703), "Mas de 11,200 libros"));
        addPOI(new ParkPOI("Cerro de Santa Maria", new LatLng(20.61240154566243, -103.38205662272627), "Bueno para correr"));
        addPOI(new RestaurantPOI("Qin ITESO", new LatLng(20.612722889645752, -103.41003742697902), "Oriental"));
        addPOI(new RestaurantPOI("Carls Jr ITESO", new LatLng(20.611932786534606, -103.41669788079429), "Hamburguesas"));
        addPOI(new RestaurantPOI("Flor de Cordoba ITESO", new LatLng(20.608081614415887, -103.41400360188156), "Cafe"));
        addPOI(new RestaurantPOI("La Esquinita", new LatLng(20.603662076426726, -103.41249322821481), "Tacos"));
        addPOI(new RestaurantPOI("Wings Army", new LatLng(20.61268151963973, -103.4159347625902), "Alitas"));
    }
    private void addPOI(POI poi) {
        // Receives POI Class and Subclasses as parameter
        // Add marker to map
        mMap.addMarker(new MarkerOptions()
                .position(poi.getLocation())
                .title(poi.getDisplayInfo()));

        // Add to poiMap for KNN
        poiMap.put(poi.getName(), poi.getLocation());
    }

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
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2, DistanceCalculator haversine) {
        return haversine.calculate(lat1, lon1, lat2, lon2);
    }
    public List<POI> findNearestPOIs(double userLat, double userLng, int k) {
        List<POI> pois = new ArrayList<>(poiMap.size()); // List to hold nearest distances and corresponding POIs
        double maxDistanceKm = 5.0; // Max distance

        DistanceCalculator haversine = (lat1, lon1, lat2, lon2) -> {
            double R = 6371; // Radius of Earth in kilometers
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        };

        // Populate the list with POIs from map
        for (Map.Entry<String, LatLng> entry : poiMap.entrySet()) {
            double distance = calculateDistance(userLat, userLng, entry.getValue().latitude, entry.getValue().longitude, haversine);

            // Check if the POI is within the maximum distance
            if (distance <= maxDistanceKm) {
                POI poi = new POI(entry.getKey(), entry.getValue());
                poi.setName(entry.getKey());
                poi.setLocation(entry.getValue());
                pois.add(poi);
            }
        }

        // Sort POIs based on distance from user location
        pois.sort(Comparator.comparingDouble(poi ->
                calculateDistance(userLat, userLng, poi.location.latitude, poi.location.longitude, haversine)));

        return pois; // Return the list of nearest POIs
    }

    @FunctionalInterface
    public interface DistanceCalculator {
        double calculate(double lat1, double lon1, double lat2, double lon2);
    }

    // Helper classes to store POI
    public class POI {
        // Attributes
        String name;
        LatLng location;

        // Constructor
        public POI(String name, LatLng location) {
            this.name = name;
            this.location = location;
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

        public String getPOIType(){
            return "Simple POI";
        };
        public String getDisplayInfo() {
            return name + " (" + getPOIType() + ")";
        }
    }
    public class RestaurantPOI extends POI {
        // Attribute
        private String cuisineType;

        // Constructor
        public RestaurantPOI(String name, LatLng location, String cuisineType) {
            super(name, location);
            this.cuisineType = cuisineType;
        }

        @Override
        public String getPOIType() {
            return cuisineType;
        }
        // getters and setters
        public String getCuisineType() {
            return cuisineType;
        }
        public void setCuisineType(String cuisineType) {
            this.cuisineType = cuisineType;
        }
    }
    public class HistoricalPOI extends POI{
        // Attribute
        private String description;

        // Constructor
        public HistoricalPOI(String name, LatLng location, String description){
            super(name, location);
            this.description = description;
        }

        @Override
        public String getPOIType(){
            return description;
        }
        // getters and setters
        public String getDescription(){ return description; }
        public void setDescription(String Description){ this.description = description; }
    }
    public class ParkPOI extends POI{
        // Attribute
        private String description;

        // Constructor
        public ParkPOI(String name, LatLng location, String description){
            super(name, location);
            this.description = description;
        }

        @Override
        public String getPOIType(){
            return description;
        }
        // getters and setters
        public String getDescription(){ return description; }
        public void setDescription(String Description){ this.description = description; }
    }

}