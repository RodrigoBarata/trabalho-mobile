package com.example.trab_mobile;

public class Satellite {
    private final int svid;
    private final int constellationType;
    private final float elevation;
    private final float azimuth;
    private final boolean usedInFix;

    public Satellite(int svid, int constellationType, float elevation, float azimuth, boolean usedInFix) {
        this.svid = svid;
        this.constellationType = constellationType;
        this.elevation = elevation;
        this.azimuth = azimuth;
        this.usedInFix = usedInFix;
    }

    public int getSvid() {
        return svid;
    }

    public int getConstellationType() {
        return constellationType;
    }

    public float getElevation() {
        return elevation;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public boolean isUsedInFix() {
        return usedInFix;
    }
}
