package de.mat2095.my_slither;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class MySlitherModel {

    public static final double PI2 = Math.PI * 2;

    final int gameRadius;
    final int sectorSize;
    final double spangdv;
    final double nsp1, nsp2, nsp3;
    private final double mamu1, mamu2;
    private final double cst;
    private final int mscps;
    private final double[] fpsls, fmlts;

    final Map<Integer, Snake> snakes = new LinkedHashMap<>();
    final Map<Integer, Prey> preys = new LinkedHashMap<>();
    final Map<Integer, Food> foods = new LinkedHashMap<>();
    final boolean[][] sectors;

    private long lastUpdateTime;

    private final MySlitherJFrame view;

    Snake snake;

    MySlitherModel(int gameRadius, int sectorSize, double spangdv, double nsp1, double nsp2, double nsp3, double mamu1, double mamu2, double cst, int mscps, MySlitherJFrame view) {
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

        fmlts = new double[mscps];
        for (int i = 0; i < mscps; i++) {
            double base = (double) (mscps - i) / mscps;
            fmlts[i] = 1 / (base * base * Math.sqrt(Math.sqrt(base)));
        }

        fpsls = new double[mscps + 1];
        for (int i = 0; i <= mscps; i++) {
            fpsls[i] = (i == 0 ? 0 : fpsls[i - 1] + fmlts[i - 1]);
        }

        lastUpdateTime = System.currentTimeMillis();
    }

    public int getSnakeLength(int bodyLength, double fillAmount) {
        bodyLength = Math.min(bodyLength, mscps);

        if (bodyLength == mscps) {
            return (int) (15 * fpsls[bodyLength] - 20);
        } else {
            return (int) (15 * (fpsls[bodyLength] + fillAmount * fmlts[bodyLength]) - 20);
        }
    }

    public void update() {
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

                if (cSnake.dir == 1) {
                    cSnake.ang -= snakeDeltaAngle;
                    cSnake.ang %= PI2;
                    if (cSnake.ang < 0) {
                        cSnake.ang += PI2;
                    }
                    double angle2go = (cSnake.wang - cSnake.ang) % PI2;
                    if (angle2go < 0) {
                        angle2go += PI2;
                    }
                    if (angle2go <= Math.PI) {
                        cSnake.ang = cSnake.wang;
                        cSnake.dir = 0;
                    }
                } else if (cSnake.dir == 2) {
                    cSnake.ang += snakeDeltaAngle;
                    cSnake.ang %= PI2;
                    if (cSnake.ang < 0) {
                        cSnake.ang += PI2;
                    }
                    double angle2go = (cSnake.wang - cSnake.ang) % PI2;
                    if (angle2go < 0) {
                        angle2go += PI2;
                    }
                    if (angle2go > Math.PI) {
                        cSnake.ang = cSnake.wang;
                        cSnake.dir = 0;
                    }
                } else {
                    cSnake.ang = cSnake.wang;
                }

                cSnake.x += Math.cos(cSnake.ang) * snakeDistance;
                cSnake.y += Math.sin(cSnake.ang) * snakeDistance;
            });

            //TODO: eahang
            double preyDeltaAngle = mamu2 * deltaTime;
            preys.values().forEach(prey -> {
                double preyDistance = prey.sp * deltaTime / 4.0;

                if (prey.dir == 1) {
                    prey.ang -= preyDeltaAngle;
                    prey.ang %= PI2;
                    if (prey.ang < 0) {
                        prey.ang += PI2;
                    }
                    double angle2go = (prey.wang - prey.ang) % PI2;
                    if (angle2go < 0) {
                        angle2go += PI2;
                    }
                    if (angle2go <= Math.PI) {
                        prey.ang = prey.wang;
                        prey.dir = 0;
                    }
                } else if (prey.dir == 2) {
                    prey.ang += preyDeltaAngle;
                    prey.ang %= PI2;
                    if (prey.ang < 0) {
                        prey.ang += PI2;
                    }
                    double angle2go = (prey.wang - prey.ang) % PI2;
                    if (angle2go < 0) {
                        angle2go += PI2;
                    }
                    if (angle2go > Math.PI) {
                        prey.ang = prey.wang;
                        prey.dir = 0;
                    }
                } else {
                    prey.ang = prey.wang;
                }

                prey.x += Math.cos(prey.ang) * preyDistance;
                prey.y += Math.sin(prey.ang) * preyDistance;
            });

            lastUpdateTime = newTime;
        }
    }

    public void addSnake(int snakeID, String name, double x, double y, double wang, double ang, double sp, double fam, Deque<SnakeBodyPart> body) {
        synchronized (view.modelLock) {
            Snake newSnake = new Snake(snakeID, name, x, y, wang, ang, sp, fam, body, this);
            if (snake == null) {
                snake = newSnake;
            }
            snakes.put(snakeID, newSnake);
        }
    }

    public Snake getSnake(int snakeID) {
        return snakes.get(snakeID);
    }

    public void removeSnake(int snakeID) {
        synchronized (view.modelLock) {
            snakes.remove(snakeID);
        }
    }

    public void addPrey(int id, double x, double y, double radius, int dir, double wang, double ang, double sp) {
        synchronized (view.modelLock) {
            preys.put(id, new Prey(x, y, radius, dir, wang, ang, sp));
        }
    }

    public Prey getPrey(int id) {
        return preys.get(id);
    }

    public void removePrey(int id) {
        synchronized (view.modelLock) {
            preys.remove(id);
        }
    }

    public void addFood(int x, int y, double size, boolean fastSpawn) {
        synchronized (view.modelLock) {
            foods.put(y * gameRadius * 3 + x, new Food(x, y, size, fastSpawn));
        }
    }

    public void removeFood(int x, int y) {
        synchronized (view.modelLock) {
            foods.remove(y * gameRadius * 3 + x);
        }
    }

    public void addSector(int x, int y) {
        synchronized (view.modelLock) {
            sectors[y][x] = true;
        }
    }

    public void removeSector(int x, int y) {
        synchronized (view.modelLock) {
            sectors[y][x] = false;
            foods.values().removeIf(f -> {
                return f.x / sectorSize == x && f.y / sectorSize == y;
            });
        }
    }
}
