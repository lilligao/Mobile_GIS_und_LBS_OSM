package com.example.mobilegisundlbsosm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_ID = 44;

    private MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private List<GeoPoint> geoPoints = new ArrayList<>();
    private Polyline line = new Polyline();   //see note below!
    private Context ctx;
    private IMapController mapController;

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



    private void getDeviceLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // check if location is enabled
        if (isLocationEnabled(locationManager)) {
            Location location = requestNewLocationData(locationManager);

            while (location == null) {
                location = requestNewLocationData(locationManager);
            }

            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setCenter(startPoint);

            geoPoints.add(startPoint);


            this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx),map);
            this.mLocationOverlay.enableMyLocation();
            map.getOverlays().add(this.mLocationOverlay);
            line.setOnClickListener(new Polyline.OnClickListener() {
                @Override
                public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                    Toast.makeText(mapView.getContext(),
                            "polyline with " + polyline.getPoints().size() + "pts was tapped",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            });

            map.getOverlayManager().add(line);

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,//GPS as provider
                1000,//update every 1 sec
                1,//every 1 m
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        geoPoints.add(startPoint);
                        //add your points here
                        line.setPoints(geoPoints);

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
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    // method to check if location is enabled
    private boolean isLocationEnabled(LocationManager locationManager) {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


}