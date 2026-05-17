package com.mindmatrix.nammarailubuddy;

import java.util.List;

final class TrainService {
    final String id;
    final String number;
    final String name;
    final String origin;
    final String destination;
    final List<StopInfo> stops;

    TrainService(String id, String number, String name, String origin, String destination, List<StopInfo> stops) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.origin = origin;
        this.destination = destination;
        this.stops = stops;
    }

    StopInfo stopFor(String stationId) {
        for (StopInfo stop : stops) {
            if (stop.stationId.equals(stationId)) {
                return stop;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return number + " - " + name;
    }
}
