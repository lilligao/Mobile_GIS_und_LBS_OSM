package com.example.mobilegisundlbsosm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.chromium.net.CronetEngine;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_ID = 44;

    private MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private List<GeoPoint> geoPoints = new ArrayList<>();
    private Polyline line = new Polyline();   //see note below!
    private Context ctx;
    private IMapController mapController;
    private Marker startMarker =null;


    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(16.0);

        getDeviceLocation();

    }



    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    private void getDeviceLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // check if location is enabled
        if (isLocationEnabled(locationManager)) {
            Location location = requestNewLocationData(locationManager);

            while (location == null) {
                location = requestNewLocationData(locationManager);
            }
            // create Geopoint of current position
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            // zoom to current position
            mapController.setCenter(startPoint);
            // Add current position to the list
            geoPoints.add(startPoint);
            // add empty polyline to the map
            map.getOverlayManager().add(line);

            // show current position
            startMarker = new Marker(map);
            startMarker.setPosition(startPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            map.getOverlays().add(startMarker);

            // set function if user click on the polyline
            line.setOnClickListener((polyline, mapView, eventPos) -> {
                Toast.makeText(mapView.getContext(),
                        "polyline with " + polyline.getActualPoints().size() + "pts was tapped",
                        Toast.LENGTH_LONG).show();
                return false;
            });


//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//            RequestPostTask task = new RequestPostTask();
//            task.PostData(startPoint);


        } else {
            // A toast provides simple feedback about an operation in a small popup.
            Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
            // An intent is an abstract description of an operation to be performed.
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @SuppressLint("MissingPermission")
    private Location requestNewLocationData(LocationManager locationManager) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.INTERNET}, PERMISSION_ID);
        }

        Location lastKnownLocation = null;
        List<String> providerNames = locationManager.getProviders(true);
        for (String provider : providerNames) {
            locationManager.requestLocationUpdates(
                    provider,//provider
                    5000,//update every 1 sec
                    3,//every 1 m
                    new LocationListener() {
                        @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
                        @Override
                        public void onLocationChanged(@NonNull Location location) {

                            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            runOnUiThread(() -> {
                                geoPoints.add(startPoint);
                                //add your points here
                                line.setPoints(geoPoints);
                                startMarker.setPosition(startPoint);
                            });

                            Thread thread = new Thread(() -> {
                                try  {
                                    Log.d("WFS-Thread runs", "thread runs");
                                    //Your code goes here
                                    RequestPostTask task = new RequestPostTask();
                                    task.PostData(startPoint);
                                } catch (Exception e) {
                                    Log.e("WFS-Thread-run", String.valueOf(e));
                                }
                            });

                            thread.start();

                            Log.d("Latitude", "disable");
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            Log.d("Latitude", "disable");
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            Log.d("Latitude", "enable");
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            Log.d("Latitude", "status");
                        }
                    }
            );
            lastKnownLocation = locationManager.getLastKnownLocation(provider);
            if (lastKnownLocation != null) {
                return lastKnownLocation;
            }
        }
        return lastKnownLocation;
    }

    // method to check if location is enabled
    private boolean isLocationEnabled(LocationManager locationManager) {
        //return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }





}