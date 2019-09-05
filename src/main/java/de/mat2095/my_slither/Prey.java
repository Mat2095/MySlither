package de.mat2095.my_slither;


class Prey {

    double x, y;
    final double radius;
    int dir;
    double wang, ang;
    double sp;

    Prey(double x, double y, double radius, int dir, double wang, double ang, double sp) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.dir = dir;
        this.wang = wang;
        this.ang = ang;
        this.sp = sp;
    }
}
