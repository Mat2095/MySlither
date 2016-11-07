package de.mat2095.my_slither;

import java.awt.Color;

public class MapPoint {

    double x, y;
    double radius;
    boolean visible;
    Color color;

    public MapPoint(double radius, Color color) {
        this.x = 0;
        this.y = 0;
        this.radius = radius;
        this.visible = false;
        this.color = color;
    }
}
