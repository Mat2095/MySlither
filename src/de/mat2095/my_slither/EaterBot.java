package de.mat2095.my_slither;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

public class EaterBot extends Player {

    private final MapPoint aim;

    public EaterBot() {
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
        double sumX = 0;
        double sumY = 0;
        double sumWeight = 0;

        Snake snake = model.snake;

        if (snake != null) {
            for (Food food : model.foods.values()) {
                double distance = Math.sqrt((food.x - snake.x) * (food.x - snake.x) + (food.y - snake.y) * (food.y - snake.y));

                double angle = Math.atan2(food.y - snake.y, food.x - snake.x);
                double deltaAngle = angle - snake.ang;
                if (deltaAngle < -Math.PI) {
                    deltaAngle += MySlitherModel.PI2;
                }
                double directionWeight = 1.25 - Math.abs(deltaAngle) / Math.PI; // gread distance: weight not important

                double sumFoodNear = model.foods.values().stream().mapToDouble(foodNear -> foodNear.getSize() / (model.sectorSize / 8 + Math.sqrt((food.x - foodNear.x) * (food.x - foodNear.x) + (food.y - foodNear.y) * (food.y - foodNear.y)))).sum();

                double weight = Math.pow(food.getSize(), 2) * Math.pow(distance + model.sectorSize / 16, -6) * Math.pow(directionWeight, 4) * Math.pow(sumFoodNear, 1);
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
