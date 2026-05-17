package com.mindmatrix.nammarailubuddy;

final class Station {
    final String id;
    final String name;
    final double latitude;
    final double longitude;

    Station(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return name;
    }
}
