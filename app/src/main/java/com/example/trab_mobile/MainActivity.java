package com.example.trab_mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "SatellitesApp";
    private SkyView skyView;
    private LocationManager locationManager;
    private GnssStatus.Callback gnssStatusCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        skyView = findViewById(R.id.skyView);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        gnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);

                Log.d(TAG, "onSatelliteStatusChanged called! Satellite count: " + status.getSatelliteCount());

                List<Satellite> satellites = new ArrayList<>();
                for (int i = 0; i < status.getSatelliteCount(); i++) {
                    satellites.add(new Satellite(
                            status.getSvid(i),
                            status.getConstellationType(i),
                            status.getElevationDegrees(i),
                            status.getAzimuthDegrees(i),
                            status.usedInFix(i)
                    ));

                    if (i == 0) { // Log only the first one to avoid spamming
                        Log.d(TAG, "Satellite 0: Azimuth=" + status.getAzimuthDegrees(i) + ", Elevation=" + status.getElevationDegrees(i));
                    }
                }

                runOnUiThread(() -> {
                    Log.d(TAG, "Updating SkyView with " + satellites.size() + " satellites.");
                    skyView.setSatellites(satellites);
                });
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationAndGnssUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationManager != null) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            locationManager.removeUpdates(this); // Stop location updates
            Log.d(TAG, "Stopped GNSS and Location updates.");
        }
    }

    private void startLocationAndGnssUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
            // Request location updates aggressively to wake up the GPS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            Log.d(TAG, "Registered for GNSS and Location updates.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationAndGnssUpdates();
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- LocationListener Methods ---
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // This log confirms the GPS is active and providing a location.
        Log.d(TAG, "onLocationChanged: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "Provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d(TAG, "Provider disabled: " + provider);
    }
}
