package de.mat2095.my_slither;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

final class MySlitherCanvas extends JPanel {

    private static final double PI2 = Math.PI * 2;

    private static final Color BACKGROUND_COLOR = new Color(0x2B2B2B);
    private static final Color FOREGROUND_COLOR = new Color(0xA9B7C6);
    private static final Color SECTOR_COLOR = new Color(0x803C3F41, true);
    private static final Color FOOD_COLOR = new Color(0xCC7832);
    private static final Color PREY_COLOR = new Color(0xFFFF00);
    private static final float[] PREY_HALO_FRACTIONS = new float[]{0.5f, 1f};
    private static final Color[] PREY_HALO_COLORS = new Color[]{new Color(0x60FFFF00, true), new Color(0x00FFFF00, true)};
    private static final Color SNAKE_COLOR = new Color(0x287BDE);
    private static final Color OWN_SNAKE_COLOR = new Color(0x39AFFF);
    private static final float[] SNAKE_HALO_FRACTIONS = new float[]{0.5f, 1f};
    private static final Color[] SNAKE_HALO_COLORS = new Color[]{new Color(0x60287BDE, true), new Color(0x00287BDE, true)};
    private static final Color[] OWN_SNAKE_HALO_COLORS = new Color[]{new Color(0x6039AFFF, true), new Color(0x0039AFFF, true)};
    private static final Color SNAKE_BODY_COLOR = new Color(0x6A8759);
    private static final Color OWN_SNAKE_BODY_COLOR = new Color(0xA5C261);
    private static final Color MAP_COLOR = new Color(0xA0A9B7C6, true);
    private static final Color MAP_POSITION_COLOR = new Color(0xE09E2927, true);

    private boolean[] map;
    private MySlitherModel model;
    private int zoom = 12;

    public MySlitherCanvas() {
        super();
        setBackground(BACKGROUND_COLOR);
        setForeground(FOREGROUND_COLOR);

        addMouseWheelListener(e -> {
            zoom -= e.getWheelRotation();
            zoom = Math.max(zoom, 0);
            zoom = Math.min(zoom, 18);
        });

        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (model != null) {
                    double atan = Math.atan2((e.getY() - getHeight() / 2), (e.getX() - getWidth() / 2));
                    model.setOwnSnakeWang((atan + PI2) % PI2);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (model != null) {
                    double atan = Math.atan2((e.getY() - getHeight() / 2), (e.getX() - getWidth() / 2));
                    model.setOwnSnakeWang((atan + PI2) % PI2);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (model != null) {
                    model.setOwnSnakeBoost(true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (model != null) {
                    model.setOwnSnakeBoost(false);
                }
            }
        });
    }

    void setModel(MySlitherModel model) {
        this.model = model;
    }

    void updateModel() {
        if (model != null) {
            model.update();
        }
    }

    void setMap(boolean[] map) {
        this.map = map;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (model == null || !(graphics instanceof Graphics2D)) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int m = Math.min(w, h);

        synchronized (model) {
            AffineTransform oldTransform = g.getTransform();
            if (zoom == 0 || model.snake == null) {
                g.translate((w - m) / 2, (h - m) / 2);
                g.scale(1d * m / (model.gameRadius * 2), 1d * m / (model.gameRadius * 2));
            } else {
                g.translate(w / 2, h / 2);
                double scale = Math.pow(1.25, zoom + 1) * m / (model.gameRadius * 2);
                g.scale(scale, scale);
                g.translate(-model.snake.x, -model.snake.y);
            }

            g.setColor(SECTOR_COLOR);
            for (int y = 0; y < model.sectors.length; y++) {
                for (int x = 0; x < model.sectors[y].length; x++) {
                    if (model.sectors[y][x]) {
                        g.fillRect(x * model.sectorSize + 1, y * model.sectorSize + 1, model.sectorSize - 2, model.sectorSize - 2);
                    }
                }
            }

            g.setColor(FOREGROUND_COLOR);
            Stroke oldStroke = g.getStroke();
            g.setStroke(new BasicStroke(128));
            g.drawOval(-64, -64, model.gameRadius * 2 + 128, model.gameRadius * 2 + 128);
            g.setStroke(oldStroke);

            g.setColor(FOOD_COLOR);
            model.foods.values().forEach(food -> {
                g.fillOval((int) (food.x - food.size / 2), (int) (food.y - food.size / 2), (int) (food.size / 1), (int) (food.size / 1));
            });

            model.preys.values().forEach(prey -> {
                g.setPaint(new RadialGradientPaint((float) (prey.x - 0.5 / g.getTransform().getScaleX()), (float) (prey.y - 0.5 / g.getTransform().getScaleY()), (float) prey.radius, PREY_HALO_FRACTIONS, PREY_HALO_COLORS));
                g.fillRect((int) Math.floor(prey.x - prey.radius - 1), (int) Math.floor(prey.y - prey.radius - 1), (int) (prey.radius * 2 + 2), (int) (prey.radius * 2 + 2));
                g.setColor(PREY_COLOR);
                g.fill(new Ellipse2D.Double(prey.x - prey.radius / 2, prey.y - prey.radius / 2, prey.radius, prey.radius));
            });

            oldStroke = g.getStroke();
            model.snakes.values().forEach(snake -> {
                double thickness = 16 + snake.body.size() / 4.0;
                if (snake.body.size() >= 2) {
                    g.setColor(snake == model.snake ? OWN_SNAKE_BODY_COLOR : SNAKE_BODY_COLOR);
                    g.setStroke(new BasicStroke((float) thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    double totalLength = 0;
                    double lastX = 0, lastY = 0;
                    for (SnakeBodyPart bodyPart : snake.body) {
                        if (bodyPart != snake.body.getFirst()) {
                            totalLength += Math.sqrt((bodyPart.x - lastX) * (bodyPart.x - lastX) + (bodyPart.y - lastY) * (bodyPart.y - lastY));
                        }
                        if (bodyPart != snake.body.getLast()) {
                            lastX = bodyPart.x;
                            lastY = bodyPart.y;
                        }
                    }

                    Path2D.Double snakePath = new Path2D.Double();
                    snakePath.moveTo(snake.x, snake.y);

                    lastX = snake.x;
                    lastY = snake.y;

                    for (SnakeBodyPart bodyPart : snake.body) {
                        double partLength = Math.sqrt((bodyPart.x - lastX) * (bodyPart.x - lastX) + (bodyPart.y - lastY) * (bodyPart.y - lastY));
                        if (partLength > totalLength) {
                            snakePath.lineTo(lastX + (totalLength / partLength) * (bodyPart.x - lastX), lastY + (totalLength / partLength) * (bodyPart.y - lastY));
                            break;
                        }
                        snakePath.lineTo(bodyPart.x, bodyPart.y);
                        totalLength -= partLength;
                        lastX = bodyPart.x;
                        lastY = bodyPart.y;
                    }

                    g.draw(snakePath);
                }

                if (snake.isBoosting()) {
                    g.setPaint(new RadialGradientPaint((float) (snake.x - 0.5 / g.getTransform().getScaleX()), (float) (snake.y - 0.5 / g.getTransform().getScaleY()), (float) (thickness * 4 / 3), SNAKE_HALO_FRACTIONS, snake == model.snake ? OWN_SNAKE_HALO_COLORS : SNAKE_HALO_COLORS));
                    g.fillRect((int) Math.round(snake.x - thickness * 3 / 2 - 1), (int) Math.round(snake.y - thickness * 3 / 2 - 1), (int) (thickness * 3 + 2), (int) (thickness * 3 + 2));
                }
                g.setColor(snake == model.snake ? OWN_SNAKE_COLOR : SNAKE_COLOR);
                g.fill(new Ellipse2D.Double(snake.x - thickness * 2 / 3, snake.y - thickness * 2 / 3, thickness * 4 / 3, thickness * 4 / 3));
            });
            g.setStroke(oldStroke);

            g.setTransform(oldTransform);

            g.setColor(MAP_COLOR);
            g.drawOval(w - 80, h - 80, 79, 79);
            if (map != null) {
                for (int i = 0; i < map.length; i++) {
                    if (map[i]) {
                        g.fillRect((i % 80) + w - 80, (i / 80) + h - 80, 1, 1);
                    }
                }
            }
            if (zoom != 0 && model.snake != null) {
                double zoomScale = Math.pow(1.25, zoom + 1);
                g.setColor(MAP_POSITION_COLOR);
                oldStroke = g.getStroke();
                g.setStroke(new BasicStroke(2));
                g.draw(new Rectangle2D.Double(
                        model.snake.x * 80 / (model.gameRadius * 2) - w / zoomScale / m * 40 + w - 80,
                        model.snake.y * 80 / (model.gameRadius * 2) - h / zoomScale / m * 40 + h - 80,
                        w / zoomScale / m * 80,
                        h / zoomScale / m * 80
                ));
                g.setStroke(oldStroke);
            }
        }
    }
}
