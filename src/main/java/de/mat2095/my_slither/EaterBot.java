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

    private static double smoothify(double value) {
        return Math.cos(value * Math.PI) / 2 + 0.5;
    }

    private void addFoodMapPoint(Food food, int color, double alpha) {
        MapPoint mapPoint = new MapPoint(16, new Color(color | (int) (alpha * 0x7F) << 24, true));
        mapPoint.x = food.x;
        mapPoint.y = food.y;
        mapPoint.visible = true;
        mapPoints.add(mapPoint);
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

                /*  TODO:
                 *  - fix shaking?
                 *  - ignore unreachable food directly behind snake
                 *  - correlation distance/direction (great distance => direction not important)
                 *  - ignore food blocked by enemies
                 *  - given two piles, chose one of them, not the middle
                 */
                
                double sizeWeight = 128 / (128 + Math.pow(food.getRadius(), 1.5));
                sizeWeight = sizeWeight * sizeWeight;
                sizeWeight = smoothify(sizeWeight);

                double distance = Math.sqrt((food.x - snake.x) * (food.x - snake.x) + (food.y - snake.y) * (food.y - snake.y));
                double distanceWeight = 1 - model.sectorSize / (model.sectorSize + distance);
                distanceWeight = smoothify(distanceWeight);

                double angle = Math.atan2(food.y - snake.y, food.x - snake.x);
                double deltaAngle = angle - snake.ang;
                if (deltaAngle < -Math.PI) {
                    deltaAngle += MySlitherModel.PI2;
                }
                double directionWeight = Math.abs(deltaAngle) / Math.PI;
                directionWeight = smoothify(directionWeight);
                directionWeight = 0.9 * Math.pow(directionWeight, 4) + 0.1;
                if (distance < model.sectorSize && directionWeight < 0.25) {
                    directionWeight = 0;
                }

                double weight = sizeWeight * distanceWeight * directionWeight;

                addFoodMapPoint(food, 0xFF8000, sizeWeight);
                addFoodMapPoint(food, 0x00FF80, distanceWeight);
                addFoodMapPoint(food, 0x8000FF, directionWeight);

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
