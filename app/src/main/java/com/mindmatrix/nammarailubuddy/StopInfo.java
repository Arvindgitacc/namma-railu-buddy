package com.mindmatrix.nammarailubuddy;

final class StopInfo {
    final String stationId;
    final String platform;
    final String arrival;
    final String generalCoach;
    final String ladiesCoach;
    final int delayMinutes;

    StopInfo(String stationId, String platform, String arrival, String generalCoach,
             String ladiesCoach, int delayMinutes) {
        this.stationId = stationId;
        this.platform = platform;
        this.arrival = arrival;
        this.generalCoach = generalCoach;
        this.ladiesCoach = ladiesCoach;
        this.delayMinutes = delayMinutes;
    }
}
