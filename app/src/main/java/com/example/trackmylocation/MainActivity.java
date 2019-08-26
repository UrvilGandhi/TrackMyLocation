package com.example.trackmylocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final PatternItem DOT = new Dot();
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(DOT);
    private GoogleMap mMap;
    private FloatingActionButton floatingActionButton;
    private SupportMapFragment mapFragment;
    private double latitude, longitude;
    private LatLng latLng;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference().child("Location");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        floatingActionButton = findViewById(R.id.floating_action_button);


        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10.0f, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    latLng = new LatLng(latitude, longitude);
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> locationList = geocoder.getFromLocation(latitude, longitude, 1);
                        for (int n = 0; n < locationList.get(0).getMaxAddressLineIndex(); n++) {

                        }
                        String str = locationList.get(0).getAddressLine(2) + ",";
                        str += locationList.get(0).getCountryName();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(str));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
                        Log.e(TAG, "onLocationChanged: " + latLng.toString() + " 1 ");
                        HashMap<String, Object> coordinatesMap = new HashMap<>();
                        coordinatesMap.put("Latitude", latitude);
                        coordinatesMap.put("Longitude", longitude);
                        coordinatesMap.put("TimeStamp", new Date().getTime());
                        String baseKey = myRef.push().getKey();
                        myRef.child(Objects.requireNonNull(baseKey)).child("Coordinates").setValue(coordinatesMap);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            });
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10.0f, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPref", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("lat", String.valueOf(latitude));
                    editor.putString("long", String.valueOf(longitude));
                    editor.apply();
                    latLng = new LatLng(latitude, longitude);
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> locationList = geocoder.getFromLocation(latitude, longitude, 2);
                        String str = locationList.get(0).getAddressLine(0);
                        mMap.addMarker(new MarkerOptions().position(latLng).title(str));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f));
                        Log.e(TAG, "onLocationChanged: " + latLng.toString() + " 2 ");
                        HashMap<String, Object> coordinatesMap = new HashMap<>();
                        coordinatesMap.put("Latitude", latitude);
                        coordinatesMap.put("Longitude", longitude);
                        coordinatesMap.put("TimeStamp", new Date().getTime());
                        coordinatesMap.put("Address", str);
                        String baseKey = myRef.push().getKey();
                        myRef.child(Objects.requireNonNull(baseKey)).child("Coordinates").setValue(coordinatesMap);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            });
        }

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                Log.d(TAG, "Value is: " + map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences preferences = getSharedPreferences("MyPref", MODE_PRIVATE);
                String lat = preferences.getString("lat", "");
                String longi = preferences.getString("long", "");
                Polyline polyline = mMap.addPolyline(new PolylineOptions()
                        .clickable(true)
                        .add(new LatLng(Double.parseDouble(Objects.requireNonNull(lat)), Double.parseDouble(Objects.requireNonNull(longi))),
                                new LatLng(24.30288, 72.47188)));
                Log.e(TAG, "onMapReady: " + lat + "," + longi);
                polyline.setTag("A");

                mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
                    @Override
                    public void onPolylineClick(Polyline polyline) {
                        if ((polyline.getPattern() == null) || (!polyline.getPattern().contains(DOT))) {
                            polyline.setPattern(PATTERN_POLYLINE_DOTTED);
                        } else {
                            // The default pattern is a solid stroke.
                            polyline.setPattern(null);
                        }

                        Toast.makeText(getApplicationContext(), "Route type " + polyline.getTag().toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


    }


}
