package de.mat2095.my_slither;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

public class EaterBot extends Player {

    private final MapPoint aim;

    private EaterBot() {
        super("EaterBot");
        aim = new MapPoint(16, new Color(0xA000FF00, true));
        mapPoints.add(aim);
    }

    public static List<Player> getPlayers() {
        List<Player> result = new LinkedList<>();
        result.add(new EaterBot());
        return result;
    }

    @Override
    public Wish action(MySlitherModel model) { // TODO: don't use model directly
        mapPoints.clear();
        mapPoints.add(aim);

        double sumX = 0;
        double sumY = 0;
        double sumWeight = 0;

        Snake snake = model.snake;

        if (snake != null) {
            for (Food food : model.foods.values()) {

                double sizeWeight = 1 - Math.pow(64 / (64 + food.getSize()), 4);//TODO: viel zu schwach

                double distance = Math.sqrt((food.x - snake.x) * (food.x - snake.x) + (food.y - snake.y) * (food.y - snake.y));
                double newDistance = 1 - model.sectorSize / (model.sectorSize + distance);
                double distanceWeight = Math.cos(newDistance * Math.PI) / 2 + 0.5;

                double angle = Math.atan2(food.y - snake.y, food.x - snake.x);
                double deltaAngle = angle - snake.ang;
                if (deltaAngle < -Math.PI) {
                    deltaAngle += MySlitherModel.PI2;
                }
                //double directionWeight = 1.25 - Math.abs(deltaAngle) / Math.PI; // gread distance: weight not important
                double directionWeight = Math.abs(deltaAngle) / Math.PI;
                directionWeight = Math.cos(directionWeight * Math.PI) / 2.0 + 0.5;
                directionWeight = Math.pow(directionWeight, 4);
                directionWeight = 0.9 * directionWeight + 0.1;
                if (distance < model.sectorSize && directionWeight < 0.25) {
                    directionWeight = 0;
                }

                MapPoint goodFoodAngle = new MapPoint(16, new Color(0, 0xFF, 0x80, (int) (sizeWeight * 0xFF)));
                goodFoodAngle.x = food.x;
                goodFoodAngle.y = food.y;
                goodFoodAngle.visible = true;
                mapPoints.add(goodFoodAngle);

//                MapPoint goodFoodDistance = new MapPoint(16, new Color(0xFF, 0, 0x80, (int) (distanceWeight * 0xFF)));
//                goodFoodDistance.x = food.x;
//                goodFoodDistance.y = food.y;
//                goodFoodDistance.visible = true;
//                mapPoints.add(goodFoodDistance);
                double weight = sizeWeight * distanceWeight * directionWeight; //TODO: sperre nach hinten, gegner dazwischen
                // zwei haufen: wähle einen, nicht die mitte!

                sumX += food.x * weight;
                sumY += food.y * weight;
                sumWeight += weight;
            }
        }

        if (sumWeight > 0) {
            aim.visible = true;
            aim.x = sumX / sumWeight;
            aim.y = sumY / sumWeight;
            return new Wish((Math.atan2(aim.y - snake.y, aim.x - snake.x) + MySlitherModel.PI2) % MySlitherModel.PI2, false);
        } else {
            aim.visible = false;
            return new Wish(null, false);
        }
    }
}
