package de.mat2095.my_slither;

public class Prey {

    double x, y;
    int dir;
    double wang, ang;
    double sp;
    private final double size;
    private final long spawnTime;

    public Prey(double x, double y, double size, int dir, double wang, double ang, double sp) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.dir = dir;
        this.wang = wang;
        this.ang = ang;
        this.sp = sp;
        this.spawnTime = System.currentTimeMillis();
    }

    public double getRadius() { // TODO: factor?
        double fillRate = (System.currentTimeMillis() - spawnTime) / 1200.0;
        if (fillRate >= 1) {
            return size;
        } else {
            return (1 - Math.cos(Math.PI * fillRate)) / 2 * size;
        }
    }
}
