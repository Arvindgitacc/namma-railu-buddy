package com.mindmatrix.nammarailubuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 31;
    private static final int REQUEST_NOTIFICATIONS = 32;
    private static final int REQUEST_BACKGROUND_LOCATION = 33;
    private static final float DESTINATION_RADIUS_METERS = 5000f;

    private final List<Station> stations = Arrays.asList(
            new Station("mandya", "Mandya", "Mysuru Passenger 06233", "2", "Front: coaches 1-3", "Middle: coach 6", 5, 12.5222, 76.9009),
            new Station("birur", "Birur", "Shivamogga Passenger 06545", "1", "Rear: coaches 10-12", "Middle: coach 5", 0, 13.5972, 75.9717),
            new Station("maddur", "Maddur", "Bengaluru Local 06582", "3", "Front: coaches 1-2", "Middle: coach 7", 12, 12.5839, 77.0435),
            new Station("ramanagara", "Ramanagara", "KSR Bengaluru MEMU 06276", "4", "Rear: coaches 9-12", "Middle: coach 6", 3, 12.7219, 77.2810),
            new Station("channapatna", "Channapatna", "Kengeri MEMU 06522", "2", "Front: coaches 1-4", "Middle: coach 8", 8, 12.6514, 77.2067),
            new Station("mysuru", "Mysuru Junction", "Bengaluru Passenger 06269", "5", "Rear: coaches 8-11", "Middle: coach 4", 0, 12.3169, 76.6461),
            new Station("bengaluru", "KSR Bengaluru", "Mandya Passenger 06270", "7", "Front: coaches 1-3", "Middle: coach 9", 15, 12.9784, 77.5697)
    );

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GeofencingClient geofencingClient;
    private Station selectedStation;
    private Station selectedDestination;
    private ListenerRegistration platformListener;
    private ListenerRegistration delayListener;

    private TextView platformText;
    private TextView coachText;
    private TextView crowdText;
    private TextView delayText;
    private EditText platformInput;
    private EditText delayInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureFirebase();
        geofencingClient = LocationServices.getGeofencingClient(this);

        selectedStation = stations.get(0);
        selectedDestination = stations.get(stations.size() - 1);

        signIn();
        requestNotificationPermission();
        setContentView(buildContent());
        bindStation(selectedStation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (platformListener != null) {
            platformListener.remove();
        }
        if (delayListener != null) {
            delayListener.remove();
        }
    }

    private View buildContent() {
        int ink = Color.rgb(16, 24, 32);
        int railGreen = Color.rgb(11, 107, 67);
        int signalYellow = Color.rgb(255, 212, 71);
        int paper = Color.rgb(247, 248, 243);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(paper);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        scrollView.addView(root);

        TextView title = text("Namma Railu Buddy", 30, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setBackgroundColor(railGreen);
        title.setPadding(dp(16), dp(18), dp(16), dp(18));
        root.addView(title, matchWrap());

        TextView subtitle = text("Passenger guide for local trains", 16, ink, false);
        subtitle.setPadding(0, dp(12), 0, dp(16));
        root.addView(subtitle);

        root.addView(label("Current station"));
        Spinner stationSpinner = spinner(stations);
        stationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStation = stations.get(position);
                bindStation(selectedStation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        root.addView(stationSpinner);

        platformText = section(root, "Platform");
        coachText = section(root, "Coach position");
        crowdText = section(root, "Passenger confirmations");
        delayText = section(root, "Delay updates");

        platformInput = input("Platform number");
        root.addView(platformInput);
        Button pingButton = button("Confirm / Update platform", signalYellow, ink);
        pingButton.setOnClickListener(v -> submitPlatformPing());
        root.addView(pingButton);

        delayInput = input("Delay in minutes");
        root.addView(delayInput);
        Button delayButton = button("Share delay update", railGreen, Color.WHITE);
        delayButton.setOnClickListener(v -> submitDelayUpdate());
        root.addView(delayButton);

        root.addView(label("Destination alarm"));
        Spinner destinationSpinner = spinner(stations);
        destinationSpinner.setSelection(stations.size() - 1);
        destinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDestination = stations.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        root.addView(destinationSpinner);

        Button alarmButton = button("Set 5 km destination alarm", railGreen, Color.WHITE);
        alarmButton.setOnClickListener(v -> setDestinationAlarm());
        root.addView(alarmButton);

        return scrollView;
    }

    private void bindStation(Station station) {
        if (platformText == null) {
            return;
        }

        platformText.setText("Platform " + station.platform + " for " + station.train);
        coachText.setText("General: " + station.generalCoach + "\nLadies: " + station.ladiesCoach);
        delayText.setText(defaultDelayText(station.delayMinutes));
        platformInput.setText(station.platform);
        delayInput.setText(String.valueOf(station.delayMinutes));
        listenForPlatformPings(station);
        listenForDelayUpdates(station);
    }

    private void listenForPlatformPings(Station station) {
        if (platformListener != null) {
            platformListener.remove();
        }
        if (db == null) {
            crowdText.setText("Firebase is not configured yet. Add google-services.json to enable live pings.");
            return;
        }
        platformListener = platformDoc(station).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                crowdText.setText("No live confirmations yet. Be the first to ping.");
                return;
            }

            String platform = snapshot.getString("platform");
            Long confirmations = snapshot.getLong("confirmations");
            if (platform == null) {
                platform = station.platform;
            }
            long count = confirmations == null ? 0 : confirmations;
            crowdText.setText(String.format(Locale.getDefault(),
                    "Platform %s confirmed by %d passengers.", platform, count));
            platformText.setText("Platform " + platform + " for " + station.train);
        });
    }

    private void listenForDelayUpdates(Station station) {
        if (delayListener != null) {
            delayListener.remove();
        }
        if (db == null) {
            delayText.setText(defaultDelayText(station.delayMinutes));
            return;
        }
        delayListener = delayDoc(station).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                delayText.setText(defaultDelayText(station.delayMinutes));
                return;
            }

            Long minutes = snapshot.getLong("minutes");
            Long reports = snapshot.getLong("reports");
            if (minutes == null) {
                delayText.setText(defaultDelayText(station.delayMinutes));
                return;
            }
            long count = reports == null ? 1 : reports;
            delayText.setText(String.format(Locale.getDefault(),
                    "Current delay: %d min, reported by %d passengers.", minutes, count));
        });
    }

    private void submitPlatformPing() {
        if (db == null) {
            toast("Add google-services.json to enable platform pings.");
            return;
        }
        String platform = platformInput.getText().toString().trim();
        if (platform.isEmpty()) {
            toast("Enter a platform number.");
            return;
        }

        DocumentReference ref = platformDoc(selectedStation);
        db.runTransaction(transaction -> {
            Long current = transaction.get(ref).getLong("confirmations");
            long next = current == null ? 1 : current + 1;
            Map<String, Object> data = new HashMap<>();
            data.put("platform", platform);
            data.put("confirmations", next);
            data.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(ref, data, SetOptions.merge());
            return null;
        }).addOnSuccessListener(unused -> toast("Platform ping shared."))
                .addOnFailureListener(e -> toast("Could not share platform ping."));
    }

    private void submitDelayUpdate() {
        if (db == null) {
            toast("Add google-services.json to enable delay updates.");
            return;
        }
        String rawDelay = delayInput.getText().toString().trim();
        if (rawDelay.isEmpty()) {
            toast("Enter delay minutes.");
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(rawDelay);
        } catch (NumberFormatException ignored) {
            toast("Use a valid number of minutes.");
            return;
        }

        DocumentReference ref = delayDoc(selectedStation);
        db.runTransaction(transaction -> {
            Long reports = transaction.get(ref).getLong("reports");
            long next = reports == null ? 1 : reports + 1;
            Map<String, Object> data = new HashMap<>();
            data.put("minutes", minutes);
            data.put("reports", next);
            data.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(ref, data, SetOptions.merge());
            return null;
        }).addOnSuccessListener(unused -> toast("Delay update shared."))
                .addOnFailureListener(e -> toast("Could not share delay update."));
    }

    private void setDestinationAlarm() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_BACKGROUND_LOCATION);
            return;
        }
        registerGeofence();
    }

    @SuppressLint("MissingPermission")
    private void registerGeofence() {
        Intent intent = new Intent(this, DestinationAlarmReceiver.class);
        intent.setAction(DestinationAlarmReceiver.ACTION_DESTINATION_GEOFENCE);
        intent.putExtra(DestinationAlarmReceiver.EXTRA_DESTINATION_NAME, selectedDestination.name);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Geofence geofence = new Geofence.Builder()
                .setRequestId("destination-" + selectedDestination.id)
                .setCircularRegion(selectedDestination.latitude, selectedDestination.longitude, DESTINATION_RADIUS_METERS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(60_000)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        geofencingClient.removeGeofences(pendingIntent)
                .addOnCompleteListener(task -> geofencingClient.addGeofences(request, pendingIntent)
                        .addOnSuccessListener(unused -> toast("Alarm set for " + selectedDestination.name + "."))
                        .addOnFailureListener(e -> toast("Could not set alarm. Check location settings.")));
    }

    private void signIn() {
        if (auth == null) {
            return;
        }
        if (auth.getCurrentUser() != null) {
            return;
        }
        auth.signInAnonymously()
                .addOnFailureListener(e -> toast("Firebase login failed. Add google-services.json and enable Anonymous Auth."));
    }

    private void configureFirebase() {
        try {
            FirebaseApp app = FirebaseApp.initializeApp(this);
            if (app != null) {
                auth = FirebaseAuth.getInstance();
                db = FirebaseFirestore.getInstance();
            }
        } catch (IllegalStateException ignored) {
            auth = null;
            db = null;
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setDestinationAlarm();
        }
        if (requestCode == REQUEST_BACKGROUND_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerGeofence();
        }
    }

    private DocumentReference platformDoc(Station station) {
        return db.collection("stations").document(station.id)
                .collection("platformPings").document(station.train.replace(" ", "_").toLowerCase(Locale.US));
    }

    private DocumentReference delayDoc(Station station) {
        return db.collection("stations").document(station.id)
                .collection("delayUpdates").document(station.train.replace(" ", "_").toLowerCase(Locale.US));
    }

    private String defaultDelayText(int minutes) {
        return minutes == 0 ? "No delay reported." : "Expected delay: " + minutes + " min.";
    }

    private TextView section(LinearLayout root, String title) {
        root.addView(label(title));
        TextView value = text("", 18, Color.rgb(16, 24, 32), true);
        value.setBackgroundColor(Color.WHITE);
        value.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(value, matchWrap());
        return value;
    }

    private TextView label(String value) {
        TextView label = text(value, 14, Color.rgb(11, 107, 67), true);
        label.setPadding(0, dp(18), 0, dp(6));
        return label;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) {
            text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        }
        text.setLineSpacing(4f, 1f);
        return text;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(Color.rgb(16, 24, 32));
        input.setHintTextColor(Color.rgb(94, 100, 106));
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        return input;
    }

    private Spinner spinner(List<Station> values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<Station> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(0, dp(4), 0, dp(4));
        return spinner;
    }

    private Button button(String value, int background, int textColor) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(16);
        button.setTextColor(textColor);
        button.setBackgroundColor(background);
        button.setAllCaps(false);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(10);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
