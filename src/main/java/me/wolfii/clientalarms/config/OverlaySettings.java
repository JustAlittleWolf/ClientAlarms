package me.wolfii.clientalarms.config;

public class OverlaySettings {
    public boolean enabled = true;
    public boolean showHeading = true;
    public String heading = "";
    public String format = "";
    public String nameFormat = "%name%";
    public OverlayAnchorX anchorX = OverlayAnchorX.LEFT;
    public OverlayAnchorY anchorY = OverlayAnchorY.TOP;
    public int offsetX = 8;
    public int offsetY = 8;
    public TextAlign align = TextAlign.LEFT;

    public static OverlaySettings at(int x, int y) {
        OverlaySettings settings = new OverlaySettings();
        settings.offsetX = x;
        settings.offsetY = y;
        return settings;
    }
}
