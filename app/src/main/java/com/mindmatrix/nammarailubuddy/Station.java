package com.mindmatrix.nammarailubuddy;

final class Station {
    final String id;
    final String name;
    final String train;
    final String platform;
    final String generalCoach;
    final String ladiesCoach;
    final int delayMinutes;
    final double latitude;
    final double longitude;

    Station(String id, String name, String train, String platform, String generalCoach,
            String ladiesCoach, int delayMinutes, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.train = train;
        this.platform = platform;
        this.generalCoach = generalCoach;
        this.ladiesCoach = ladiesCoach;
        this.delayMinutes = delayMinutes;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return name;
    }
}
