package com.varestorm.plugins.teleports.teleporting;

public class AnchorPoint {

    Telepoint telepoint;

    public AnchorPoint(Telepoint telepoint) {
        this.telepoint = telepoint;
    }

    public Telepoint getTelepoint() {
        return this.telepoint;
    }
}
