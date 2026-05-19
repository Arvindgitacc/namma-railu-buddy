package com.mindmatrix.nammarailubuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
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

import java.util.ArrayList;
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

    private static final int INK = 0xFF101820;
    private static final int RAIL_GREEN = 0xFF0B6B43;
    private static final int SIGNAL_YELLOW = 0xFFFFD447;
    private static final int PAPER = 0xFFF7F8F3;
    private static final int MUTED = 0xFF5E646A;
    private static final int BORDER = 0xFFE1E5DD;

    private final List<Station> stations = Arrays.asList(
            new Station("mandya", "Mandya", 12.5222, 76.9009),
            new Station("maddur", "Maddur", 12.5839, 77.0435),
            new Station("channapatna", "Channapatna", 12.6514, 77.2067),
            new Station("ramanagara", "Ramanagara", 12.7219, 77.2810),
            new Station("bengaluru", "KSR Bengaluru", 12.9784, 77.5697),
            new Station("mysuru", "Mysuru Junction", 12.3169, 76.6461),
            new Station("birur", "Birur", 13.5972, 75.9717)
    );

    private final List<TrainService> trains = Arrays.asList(
            new TrainService("mysuru-passenger-06233", "06233", "Mysuru Passenger", "KSR Bengaluru", "Mysuru Junction", Arrays.asList(
                    new StopInfo("bengaluru", "7", "06:20", "Rear: coaches 9-12", "Middle: coach 6", 0),
                    new StopInfo("ramanagara", "4", "07:18", "Rear: coaches 9-12", "Middle: coach 6", 3),
                    new StopInfo("channapatna", "2", "07:36", "Front: coaches 1-4", "Middle: coach 8", 4),
                    new StopInfo("maddur", "3", "08:02", "Front: coaches 1-2", "Middle: coach 7", 8),
                    new StopInfo("mandya", "2", "08:28", "Front: coaches 1-3", "Middle: coach 6", 5),
                    new StopInfo("mysuru", "5", "09:35", "Rear: coaches 8-11", "Middle: coach 4", 0)
            )),
            new TrainService("bengaluru-memu-06276", "06276", "Bengaluru MEMU", "Mysuru Junction", "KSR Bengaluru", Arrays.asList(
                    new StopInfo("mysuru", "4", "17:10", "Front: coaches 1-3", "Middle: coach 5", 0),
                    new StopInfo("mandya", "1", "18:03", "Rear: coaches 8-11", "Middle: coach 6", 6),
                    new StopInfo("maddur", "2", "18:28", "Rear: coaches 8-10", "Middle: coach 5", 10),
                    new StopInfo("channapatna", "1", "18:49", "Front: coaches 1-4", "Middle: coach 7", 7),
                    new StopInfo("ramanagara", "3", "19:08", "Front: coaches 1-2", "Middle: coach 6", 3),
                    new StopInfo("bengaluru", "6", "20:05", "Rear: coaches 9-12", "Middle: coach 8", 0)
            )),
            new TrainService("shivamogga-passenger-06545", "06545", "Shivamogga Passenger", "Birur", "KSR Bengaluru", Arrays.asList(
                    new StopInfo("birur", "1", "05:55", "Rear: coaches 10-12", "Middle: coach 5", 0),
                    new StopInfo("bengaluru", "8", "10:45", "Front: coaches 1-3", "Middle: coach 7", 12)
            )),
            new TrainService("mandya-passenger-06270", "06270", "Mandya Passenger", "KSR Bengaluru", "Mandya", Arrays.asList(
                    new StopInfo("bengaluru", "5", "18:35", "Front: coaches 1-3", "Middle: coach 9", 15),
                    new StopInfo("ramanagara", "2", "19:22", "Rear: coaches 9-12", "Middle: coach 6", 8),
                    new StopInfo("channapatna", "3", "19:40", "Rear: coaches 8-10", "Middle: coach 5", 6),
                    new StopInfo("maddur", "1", "20:04", "Front: coaches 1-2", "Middle: coach 7", 3),
                    new StopInfo("mandya", "2", "20:31", "Front: coaches 1-4", "Middle: coach 6", 0)
            ))
    );

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GeofencingClient geofencingClient;
    private Station selectedStation;
    private Station selectedDestination;
    private TrainService selectedTrain;
    private StopInfo selectedStop;
    private ListenerRegistration platformListener;
    private ListenerRegistration delayListener;

    private Spinner trainSpinner;
    private Spinner destinationSpinner;
    private TextView trainSummaryText;
    private TextView routeText;
    private TextView platformText;
    private TextView arrivalText;
    private TextView coachText;
    private TextView crowdText;
    private TextView delayText;
    private EditText platformInput;
    private EditText delayInput;
    private long localPlatformConfirmations;
    private long localDelayReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureFirebase();
        geofencingClient = LocationServices.getGeofencingClient(this);

        selectedStation = stations.get(0);
        selectedDestination = findStation("bengaluru");
        selectedTrain = trainsForStation(selectedStation).get(0);
        selectedStop = selectedTrain.stopFor(selectedStation.id);

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
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PAPER);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        scrollView.addView(root);

        LinearLayout header = panel(RAIL_GREEN, RAIL_GREEN, 0);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView title = text("Namma Railu Buddy", 29, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, matchWrap());
        root.addView(header, matchWrap());

        TextView subtitle = text("Passenger guide for local trains", 16, INK, false);
        subtitle.setPadding(0, dp(12), 0, dp(12));
        root.addView(subtitle);

        LinearLayout journeyPanel = sectionPanel(root, "Plan your journey");
        journeyPanel.addView(label("Current station"));
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
        journeyPanel.addView(stationSpinner);

        journeyPanel.addView(label("Select train"));
        trainSpinner = spinner(new ArrayList<>(trainsForStation(selectedStation)));
        trainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTrain = (TrainService) parent.getItemAtPosition(position);
                bindTrain(selectedTrain);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        journeyPanel.addView(trainSpinner);

        trainSummaryText = cardText(journeyPanel, "", 18, true);
        routeText = infoLine(journeyPanel, "");

        LinearLayout board = sectionPanel(root, "Live station board");
        platformText = cardText(board, "", 21, true);
        arrivalText = cardText(board, "", 18, true);
        coachText = cardText(board, "", 18, true);
        crowdText = cardText(board, "", 17, false);
        delayText = cardText(board, "", 17, false);

        LinearLayout updates = sectionPanel(root, "Community updates");
        updates.addView(label("Platform ping"));
        platformInput = input("Platform number");
        updates.addView(platformInput);
        Button pingButton = button("Confirm / Update platform", SIGNAL_YELLOW, INK);
        pingButton.setOnClickListener(v -> submitPlatformPing());
        updates.addView(pingButton);

        updates.addView(label("Delay report"));
        delayInput = input("Delay in minutes");
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        updates.addView(delayInput);
        Button delayButton = button("Share delay update", RAIL_GREEN, Color.WHITE);
        delayButton.setOnClickListener(v -> submitDelayUpdate());
        updates.addView(delayButton);

        LinearLayout alarm = sectionPanel(root, "Destination alarm");
        alarm.addView(label("Wake me near"));
        destinationSpinner = spinner(stations);
        destinationSpinner.setSelection(stations.indexOf(selectedDestination));
        destinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDestination = stations.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        alarm.addView(destinationSpinner);

        Button alarmButton = button("Set 5 km destination alarm", RAIL_GREEN, Color.WHITE);
        alarmButton.setOnClickListener(v -> setDestinationAlarm());
        alarm.addView(alarmButton);

        return scrollView;
    }

    private void bindStation(Station station) {
        if (trainSpinner == null) {
            return;
        }

        List<TrainService> stationTrains = trainsForStation(station);
        if (!stationTrains.contains(selectedTrain)) {
            selectedTrain = stationTrains.get(0);
        }

        ArrayAdapter<TrainService> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stationTrains);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trainSpinner.setAdapter(adapter);
        trainSpinner.setSelection(stationTrains.indexOf(selectedTrain));
        bindTrain(selectedTrain);
    }

    private void bindTrain(TrainService train) {
        if (platformText == null) {
            return;
        }

        selectedStop = train.stopFor(selectedStation.id);
        if (selectedStop == null) {
            selectedStop = train.stops.get(0);
        }

        selectedDestination = destinationFor(train);
        if (destinationSpinner != null) {
            int destinationIndex = stations.indexOf(selectedDestination);
            if (destinationIndex >= 0 && destinationSpinner.getSelectedItemPosition() != destinationIndex) {
                destinationSpinner.setSelection(destinationIndex);
            }
        }

        trainSummaryText.setText(train.number + " " + train.name + "\n" + train.origin + " to " + train.destination);
        routeText.setText(String.format(Locale.getDefault(),
                "%s stop: %s passenger guidance is ready.", selectedStation.name, train.number));
        platformText.setText("Platform " + selectedStop.platform + " for " + train.name + " " + train.number);
        arrivalText.setText("Expected arrival: " + selectedStop.arrival);
        coachText.setText("General: " + selectedStop.generalCoach + "\nLadies: " + selectedStop.ladiesCoach);
        crowdText.setText("No live confirmations yet. Be the first to ping.");
        delayText.setText(defaultDelayText(selectedStop.delayMinutes));
        platformInput.setText(selectedStop.platform);
        delayInput.setText(String.valueOf(selectedStop.delayMinutes));
        localPlatformConfirmations = 0;
        localDelayReports = 0;
        listenForPlatformPings();
        listenForDelayUpdates();
    }

    private void listenForPlatformPings() {
        if (platformListener != null) {
            platformListener.remove();
        }
        if (db == null) {
            crowdText.setText("No live confirmations yet. Your update will be saved on this device.");
            return;
        }
        platformListener = platformDoc().addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                crowdText.setText("No live confirmations yet. Be the first to ping.");
                return;
            }

            String platform = snapshot.getString("platform");
            Long confirmations = snapshot.getLong("confirmations");
            if (platform == null) {
                platform = selectedStop.platform;
            }
            long count = confirmations == null ? 0 : confirmations;
            crowdText.setText(String.format(Locale.getDefault(),
                    "Platform %s confirmed by %d passengers.", platform, count));
            platformText.setText("Platform " + platform + " for " + selectedTrain.name + " " + selectedTrain.number);
        });
    }

    private void listenForDelayUpdates() {
        if (delayListener != null) {
            delayListener.remove();
        }
        if (db == null) {
            delayText.setText(defaultDelayText(selectedStop.delayMinutes));
            return;
        }
        delayListener = delayDoc().addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                delayText.setText(defaultDelayText(selectedStop.delayMinutes));
                return;
            }

            Long minutes = snapshot.getLong("minutes");
            Long reports = snapshot.getLong("reports");
            if (minutes == null) {
                delayText.setText(defaultDelayText(selectedStop.delayMinutes));
                return;
            }
            long count = reports == null ? 1 : reports;
            delayText.setText(String.format(Locale.getDefault(),
                    "Current delay: %d min, reported by %d passengers.", minutes, count));
        });
    }

    private void submitPlatformPing() {
        String platform = platformInput.getText().toString().trim();
        if (platform.isEmpty()) {
            toast("Enter a platform number.");
            return;
        }

        if (db == null) {
            localPlatformConfirmations++;
            crowdText.setText(String.format(Locale.getDefault(),
                    "Platform %s confirmed by %d passengers on this device.", platform, localPlatformConfirmations));
            platformText.setText("Platform " + platform + " for " + selectedTrain.name + " " + selectedTrain.number);
            toast("Platform update saved on this device.");
            return;
        }

        DocumentReference ref = platformDoc();
        db.runTransaction(transaction -> {
            Long current = transaction.get(ref).getLong("confirmations");
            long next = current == null ? 1 : current + 1;
            Map<String, Object> data = new HashMap<>();
            data.put("platform", platform);
            data.put("confirmations", next);
            data.put("train", selectedTrain.number);
            data.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(ref, data, SetOptions.merge());
            return null;
        }).addOnSuccessListener(unused -> toast("Platform ping shared."))
                .addOnFailureListener(e -> toast("Could not share platform ping."));
    }

    private void submitDelayUpdate() {
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

        if (db == null) {
            localDelayReports++;
            delayText.setText(String.format(Locale.getDefault(),
                    "Current delay: %d min, reported by %d passengers on this device.", minutes, localDelayReports));
            toast("Delay update saved on this device.");
            return;
        }

        DocumentReference ref = delayDoc();
        db.runTransaction(transaction -> {
            Long reports = transaction.get(ref).getLong("reports");
            long next = reports == null ? 1 : reports + 1;
            Map<String, Object> data = new HashMap<>();
            data.put("minutes", minutes);
            data.put("reports", next);
            data.put("train", selectedTrain.number);
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

    private DocumentReference platformDoc() {
        return db.collection("stations").document(selectedStation.id)
                .collection("platformPings").document(selectedTrain.id);
    }

    private DocumentReference delayDoc() {
        return db.collection("stations").document(selectedStation.id)
                .collection("delayUpdates").document(selectedTrain.id);
    }

    private List<TrainService> trainsForStation(Station station) {
        List<TrainService> result = new ArrayList<>();
        for (TrainService train : trains) {
            if (train.stopFor(station.id) != null) {
                result.add(train);
            }
        }
        return result;
    }

    private Station destinationFor(TrainService train) {
        for (Station station : stations) {
            if (station.name.equals(train.destination)) {
                return station;
            }
        }
        return selectedDestination == null ? stations.get(0) : selectedDestination;
    }

    private Station findStation(String id) {
        for (Station station : stations) {
            if (station.id.equals(id)) {
                return station;
            }
        }
        return stations.get(0);
    }

    private String defaultDelayText(int minutes) {
        return minutes == 0 ? "No delay reported." : "Expected delay: " + minutes + " min.";
    }

    private LinearLayout sectionPanel(LinearLayout root, String title) {
        TextView heading = text(title, 19, RAIL_GREEN, true);
        heading.setPadding(0, dp(18), 0, dp(8));
        root.addView(heading);

        LinearLayout panel = panel(Color.WHITE, BORDER, 1);
        panel.setPadding(dp(14), dp(12), dp(14), dp(14));
        root.addView(panel, matchWrap());
        return panel;
    }

    private LinearLayout panel(int fillColor, int strokeColor, int strokeWidth) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(box(fillColor, strokeColor, strokeWidth, 8));
        return panel;
    }

    private TextView cardText(LinearLayout root, String value, int sp, boolean bold) {
        TextView text = text(value, sp, INK, bold);
        text.setBackground(box(PAPER, BORDER, 1, 6));
        text.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(8);
        root.addView(text, params);
        return text;
    }

    private TextView infoLine(LinearLayout root, String value) {
        TextView text = text(value, 16, MUTED, false);
        text.setPadding(0, dp(8), 0, 0);
        root.addView(text, matchWrap());
        return text;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, RAIL_GREEN, true);
        label.setAllCaps(true);
        label.setLetterSpacing(0.08f);
        label.setPadding(0, dp(10), 0, dp(5));
        return label;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        text.setLineSpacing(3f, 1f);
        return text;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        return input;
    }

    private <T> Spinner spinner(List<T> values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<T> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
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
        button.setAllCaps(false);
        button.setBackground(box(background, background, 0, 6));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(10);
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable box(int fillColor, int strokeColor, int strokeWidth, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeWidth > 0) {
            drawable.setStroke(dp(strokeWidth), strokeColor);
        }
        return drawable;
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
