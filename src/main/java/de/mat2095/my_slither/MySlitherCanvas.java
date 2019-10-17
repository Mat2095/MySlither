package de.mat2095.my_slither;

import static de.mat2095.my_slither.MySlitherModel.PI2;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;


final class MySlitherCanvas extends JPanel {

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
    private static final Color NAME_SHADOW_COLOR = new Color(0xC02B2B2B, true);
    private static final Font NAME_FONT = Font.decode("SansSerif-BOLD");
    private static final Font DEBUG_FONT = Font.decode("SansSerif-PLAIN-12");

    private boolean[] map;
    private final MySlitherJFrame view;
    private int zoom = 12;
    private long lastFrameTime;
    private double fps;
    final ScheduledExecutorService repaintThread;

    final MouseInput mouseInput = new MouseInput();

    class MouseInput extends Player {

        Double wang;
        boolean boost;

        private MouseInput() {
            super("Mouse Input");
            wang = null;
            boost = false;
        }

        private void readWang(MouseEvent e) {
            wang = (Math.atan2((e.getY() - getHeight() / 2), (e.getX() - getWidth() / 2)) + PI2) % PI2;
        }

        @Override
        public Wish action(MySlitherModel model) {
            return new Wish(wang, boost);
        }
    }

    MySlitherCanvas(MySlitherJFrame view) {
        super();
        this.view = view;

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
                mouseInput.readWang(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseInput.readWang(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseInput.boost = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseInput.boost = false;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseInput.wang = null;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mouseInput.readWang(e);
            }
        });

        GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int refreshRate = localGraphicsEnvironment.getDefaultScreenDevice().getDisplayMode().getRefreshRate();
        long repaintDelay = 1000000000 / (refreshRate != DisplayMode.REFRESH_RATE_UNKNOWN ? refreshRate : 60);
        repaintThread = Executors.newSingleThreadScheduledExecutor();
        repaintThread.scheduleAtFixedRate(this::repaint, 1, repaintDelay, TimeUnit.NANOSECONDS);
    }

    void setMap(boolean[] map) {
        this.map = map;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (!(graphics instanceof Graphics2D)) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int m = Math.min(w, h);

        modelPaintBlock:
        synchronized (view.modelLock) {
            MySlitherModel model = view.model;
            if (model == null) {
                break modelPaintBlock;
            }

            AffineTransform oldTransform = g.getTransform();
            double scale;
            if (zoom == 0 || model.snake == null) {
                g.translate((w - m) / 2, (h - m) / 2);
                scale = 1d * m / (model.gameRadius * 2);
                g.scale(scale, scale);
            } else {
                g.translate(w / 2, h / 2);
                scale = Math.pow(1.25, zoom + 1) * m / (model.gameRadius * 2);
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
                double foodRadius = food.getRadius();
                g.fill(new Ellipse2D.Double(food.x - foodRadius, food.y - foodRadius, foodRadius * 2, foodRadius * 2));
            });

            model.preys.values().forEach(prey -> {
                double preyRadius = prey.getRadius();
                if (preyRadius <= 0) {
                    return;
                }
                g.setPaint(new RadialGradientPaint((float) (prey.x - 0.5 / scale), (float) (prey.y - 0.5 / scale), (float) (preyRadius * 2), PREY_HALO_FRACTIONS, PREY_HALO_COLORS));
                g.fillRect((int) Math.floor(prey.x - preyRadius * 2 - 1), (int) Math.floor(prey.y - preyRadius * 2 - 1), (int) (preyRadius * 4 + 3), (int) (preyRadius * 4 + 3));
                g.setColor(PREY_COLOR);
                g.fill(new Ellipse2D.Double(prey.x - preyRadius, prey.y - preyRadius, preyRadius * 2, preyRadius * 2));
            });

            oldStroke = g.getStroke();
            g.setFont(NAME_FONT.deriveFont((float) (18 / Math.pow(scale, 0.75))));
            model.snakes.values().forEach(snake -> {
                double thickness = 16 + snake.body.size() / 4.0;
                if (snake.body.size() >= 2) {
                    g.setColor(snake == model.snake ? OWN_SNAKE_BODY_COLOR : SNAKE_BODY_COLOR);
                    g.setStroke(new BasicStroke((float) thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    double totalLength = 0; // TODO: respect FAM, ???
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
                    g.setPaint(new RadialGradientPaint((float) (snake.x - 0.5 / scale), (float) (snake.y - 0.5 / scale),
                        (float) (thickness * 4 / 3), SNAKE_HALO_FRACTIONS,
                        snake == model.snake ? OWN_SNAKE_HALO_COLORS : SNAKE_HALO_COLORS));
                    g.fillRect((int) Math.round(snake.x - thickness * 3 / 2 - 1), (int) Math.round(snake.y - thickness * 3 / 2 - 1), (int) (thickness * 3 + 2), (int) (thickness * 3 + 2));
                }
                g.setColor(snake == model.snake ? OWN_SNAKE_COLOR : SNAKE_COLOR);
                g.fill(new Ellipse2D.Double(snake.x - thickness * 2 / 3, snake.y - thickness * 2 / 3, thickness * 4 / 3, thickness * 4 / 3));

                String lengthText = "" + model.getSnakeLength(snake.body.size(), snake.getFam());

                g.setColor(NAME_SHADOW_COLOR);
                g.drawString(snake.name,
                    (float) (snake.x - g.getFontMetrics().stringWidth(snake.name) / 2.0 + g.getFontMetrics().getHeight() / 12.0),
                    (float) (snake.y - thickness * 2 / 3 - g.getFontMetrics().getHeight() + g.getFontMetrics().getHeight() / 12.0));
                g.drawString(lengthText,
                    (float) (snake.x - g.getFontMetrics().stringWidth(lengthText) / 2.0 + g.getFontMetrics().getHeight() / 12.0),
                    (float) (snake.y - thickness * 2 / 3 + g.getFontMetrics().getHeight() / 12.0));

                g.setColor(FOREGROUND_COLOR);
                g.drawString(snake.name, (float) (snake.x - g.getFontMetrics().stringWidth(snake.name) / 2.0), (float) (snake.y - thickness * 2 / 3 - g.getFontMetrics().getHeight()));
                g.drawString(lengthText, (float) (snake.x - g.getFontMetrics().stringWidth(lengthText) / 2.0), (float) (snake.y - thickness * 2 / 3));
            });
            g.setStroke(oldStroke);

            g.setTransform(oldTransform);

            g.setColor(MAP_COLOR);
            g.drawOval(w - 80, h - 80, 79, 79);
            boolean[] currentMap = map; // TODO: save map in model, set to null on reconnect
            if (currentMap != null) {
                for (int i = 0; i < currentMap.length; i++) {
                    if (currentMap[i]) {
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

        g.setFont(DEBUG_FONT);
        g.setColor(FOREGROUND_COLOR);
        long newFrameTime = System.currentTimeMillis();
        if (newFrameTime > lastFrameTime) {
            fps = 0.95 * fps + 0.05 * 1000.0 / (newFrameTime - lastFrameTime);
        }
        g.drawString("FPS: " + Math.round(fps), 0, g.getFontMetrics().getAscent());
        lastFrameTime = newFrameTime;
    }
}
