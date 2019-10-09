package de.mat2095.my_slither;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

class MySlitherModel {

    static final double PI2 = Math.PI * 2;

    final int gameRadius;
    final int sectorSize;
    final double spangdv;
    final double nsp1, nsp2, nsp3;
    private final double mamu1, mamu2;
    private final double cst; // TODO: usage?
    private final int mscps;
    private final double[] fpsls, fmlts;

    final Map<Integer, Snake> snakes = new LinkedHashMap<>();
    final Map<Integer, Prey> preys = new LinkedHashMap<>();
    final Map<Integer, Food> foods = new LinkedHashMap<>();
    final boolean[][] sectors;

    private long lastUpdateTime;

    private final MySlitherJFrame view;

    Snake snake;

    MySlitherModel(int gameRadius, int sectorSize, double spangdv, double nsp1, double nsp2, double nsp3, double mamu1,
            double mamu2, double cst, int mscps, MySlitherJFrame view) {
        this.gameRadius = gameRadius;
        this.sectorSize = sectorSize;
        this.spangdv = spangdv;
        this.nsp1 = nsp1;
        this.nsp2 = nsp2;
        this.nsp3 = nsp3;
        this.mamu1 = mamu1;
        this.mamu2 = mamu2;
        this.cst = cst;
        this.mscps = mscps;
        this.view = view;

        sectors = new boolean[gameRadius * 2 / sectorSize][gameRadius * 2 / sectorSize];

        fmlts = new double[mscps + 1];
        fpsls = new double[mscps + 1];

        for (int i = 0; i < mscps; i++) {
            double base = (double) (mscps - i) / mscps;
            fmlts[i] = 1 / (base * base * Math.sqrt(Math.sqrt(base)));
            fpsls[i + 1] = fpsls[i] + fmlts[i];
        }

        lastUpdateTime = System.currentTimeMillis();
    }

    int getSnakeLength(int bodyLength, double fillAmount) {
        bodyLength = Math.min(bodyLength, mscps);
        return (int) (15 * (fpsls[bodyLength] + fillAmount * fmlts[bodyLength]) - 20);
    }

    void update() {
        synchronized (view.modelLock) {
            long newTime = System.currentTimeMillis();

            double deltaTimeWIP = (newTime - lastUpdateTime) / 8.0;
            deltaTimeWIP = Math.min(deltaTimeWIP, 5.0);
            final double deltaTime = deltaTimeWIP;

            snakes.values().forEach(cSnake -> {

                double snakeDeltaAngle = mamu1 * deltaTime * cSnake.getScang() * cSnake.getSpang();
                double snakeDistance = cSnake.sp * deltaTime / 4.0;

                if (snakeDistance > 42) {
                    snakeDistance = 42;
                }

                if (cSnake.tsp != cSnake.sp) {
                    if (cSnake.tsp < cSnake.sp) {
                        cSnake.tsp += 0.3;
                        if (cSnake.tsp > cSnake.sp) {
                            cSnake.tsp = cSnake.sp;
                        }
                    } else {
                        cSnake.tsp -= 0.3;
                        if (cSnake.tsp < cSnake.sp) {
                            cSnake.tsp = cSnake.sp;
                        }
                    }
                }

                if (cSnake.dir != 1 || cSnake.dir != 2)
                    cSnake.ang = cSnake.wang;
                else 
                    cSnake = snakeAngleToTake(cSnake, snakeDeltaAngle);

                cSnake.x += Math.cos(cSnake.ang) * snakeDistance;
                cSnake.y += Math.sin(cSnake.ang) * snakeDistance;
            });

            // TODO: eahang
            double preyDeltaAngle = mamu2 * deltaTime;
            preys.values().forEach(prey -> {
                double preyDistance = prey.sp * deltaTime / 4.0;
                
                if (prey.dir != 1 || prey.dir != 2)
                    prey.ang = prey.wang;
                else 
                    prey = preyAngleToTake(prey, preyDeltaAngle);

                prey.x += Math.cos(prey.ang) * preyDistance;
                prey.y += Math.sin(prey.ang) * preyDistance;
            });

            lastUpdateTime = newTime;
        }
    }

    // TODO: ? Have snake and prey inherit from class to eliminate need for seperate functions
    private Snake snakeAngleToTake(Snake cSnake, double snakeDeltaAngle) {

        if (cSnake.dir == 1)
            cSnake.ang -= snakeDeltaAngle;
        else
            cSnake.ang += snakeDeltaAngle;

        cSnake.ang %= PI2;

        if (cSnake.ang < 0) {
            cSnake.ang += PI2;
        }

        double angle2go = (cSnake.wang - cSnake.ang) % PI2;
        
        boolean b;
        if (cSnake.dir == 1)
            b = angle2go <= Math.PI ? true : false;
        else
            b = angle2go > Math.PI ? true : false;

        if (b) {
            cSnake.ang = cSnake.wang;
            cSnake.dir = 0;
        }

        return cSnake;
    }

    private Prey preyAngleToTake(Prey prey, double preyDeltaAngle) {
              
        if (prey.dir == 1) 
            prey.ang -= preyDeltaAngle;
        else 
            prey.ang += preyDeltaAngle;
            
        prey.ang %= PI2;

            if (prey.ang < 0) {
                prey.ang += PI2;
            }

            double angle2go = (prey.wang - prey.ang) % PI2;
            if (angle2go < 0) {
                angle2go += PI2;
            }

            boolean b;
            if (prey.dir == 1)
                b = angle2go <= Math.PI ? true : false;
            else
                b = angle2go > Math.PI ? true : false;
            
            if (b) {
                prey.ang = prey.wang;
                prey.dir = 0;
            }
            return prey;
    }


    void addSnake(int snakeID, String name, double x, double y, double wang, double ang, double sp, double fam,
            Deque<SnakeBodyPart> body) {
        synchronized (view.modelLock) {
            Snake newSnake = new Snake(snakeID, name, x, y, wang, ang, sp, fam, body, this);
            if (snake == null) {
                snake = newSnake;
            }
            snakes.put(snakeID, newSnake);
        }
    }

    Snake getSnake(int snakeID) {
        return snakes.get(snakeID);
    }

    void removeSnake(int snakeID) {
        synchronized (view.modelLock) {
            snakes.remove(snakeID);
        }
    }

    void addPrey(int id, double x, double y, double radius, int dir, double wang, double ang, double sp) {
        synchronized (view.modelLock) {
            preys.put(id, new Prey(x, y, radius, dir, wang, ang, sp));
        }
    }

    Prey getPrey(int id) {
        return preys.get(id);
    }

    void removePrey(int id) {
        synchronized (view.modelLock) {
            preys.remove(id);
        }
    }

    void addFood(int x, int y, double size, boolean fastSpawn) {
        synchronized (view.modelLock) {
            foods.put(y * gameRadius * 3 + x, new Food(x, y, size, fastSpawn));
        }
    }

    void removeFood(int x, int y) {
        synchronized (view.modelLock) {
            foods.remove(y * gameRadius * 3 + x);
        }
    }

    void addSector(int x, int y) {
        synchronized (view.modelLock) {
            sectors[y][x] = true;
        }
    }

    void removeSector(int x, int y) {
        synchronized (view.modelLock) {
            sectors[y][x] = false;
            foods.values().removeIf(f -> {
                return f.x / sectorSize == x && f.y / sectorSize == y;
            });
        }
    }
}
