package de.mat2095.my_slither;

import static de.mat2095.my_slither.MySlitherModel.PI2;


abstract class Player {

    final String name;

    public Player(String name) {
        this.name = name;
    }

    public abstract Wish action(MySlitherModel model);

    public static class Wish {

        final Double angle;
        final Boolean boost;

        public Wish(Double angle, Boolean boost) {
            if (angle != null && (angle < 0 || angle >= PI2)) {
                throw new IllegalArgumentException("angle not in range 0 to PI2");
            }
            this.angle = angle;
            this.boost = boost;
        }
    }
}
