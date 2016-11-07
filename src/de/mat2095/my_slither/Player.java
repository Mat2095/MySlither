package de.mat2095.my_slither;

import static de.mat2095.my_slither.MySlitherModel.PI2;
import java.util.LinkedList;
import java.util.List;

public abstract class Player {

    public final String name;
    public final List<MapPoint> mapPoints;

    public Player(String name) {
        this.name = name;
        this.mapPoints = new LinkedList<>();
    }

    public abstract Wish action(MySlitherModel model);

    public static class Wish {

        public final Double angle;
        public final Boolean boost;

        public Wish(Double angle, Boolean boost) {
            if (angle != null && (angle < 0 || angle >= PI2)) {
                throw new IllegalArgumentException("angle not in range 0 to PI2");
            }
            this.angle = angle;
            this.boost = boost;
        }
    }
}
