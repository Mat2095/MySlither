package de.mat2095.my_slither;

import java.awt.*;


class MapPoint {

    double x, y;
    double radius;
    boolean visible;
    Color color;

    MapPoint(double radius, Color color) {
        this.x = 0;
        this.y = 0;
        this.radius = radius;
        this.visible = false;
        this.color = color;
    }
}
