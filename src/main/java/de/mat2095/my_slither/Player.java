package de.mat2095.my_slither;

import static de.mat2095.my_slither.MySlitherModel.PI2;

import java.util.LinkedList;
import java.util.List;


abstract class Player {

    final String name;
    final List<MapPoint> mapPoints;

    Player(String name) {
        this.name = name;
        this.mapPoints = new LinkedList<>();
    }

    public abstract Wish action(MySlitherModel model);

    static class Wish {

        final Double angle;
        final Boolean boost;

        Wish(Double angle, Boolean boost) {
            if (angle != null && (angle < 0 || angle >= PI2)) {
                throw new IllegalArgumentException("angle not in range 0 to PI2");
            }
            this.angle = angle;
            this.boost = boost;
        }
    }
}
