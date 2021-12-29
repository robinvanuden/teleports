package com.varestorm.plugins.teleports.teleporting;

public class BedPoint {

    Telepoint telepoint;

    public BedPoint(Telepoint telepoint) {
        this.telepoint = telepoint;
    }

    public Telepoint getTelepoint() {
        return this.telepoint;
    }
}
