package de.mat2095.my_slither;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

final class MySlitherJFrame extends JFrame {

    private static final String[] SNAKES = new String[]{
        "00 - purple",
        "01 - blue",
        "02 - cyan",
        "03 - green",
        "04 - yellow",
        "05 - orange",
        "06 - salmon",
        "07 - red",
        "08 - violet",
        "09 - flag: USA",
        "10 - flag: Russia",
        "11 - flag: Germany",
        "12 - flag: Italy",
        "13 - flag: France",
        "14 - white/red",
        "15 - rainbow",
        "16 - blue/yellow",
        "17 - white/blue",
        "18 - red/white",
        "19 - white",
        "20 - green/purple",
        "21 - flag: Brazil",
        "22 - flag: Ireland",
        "23 - flag: Romania",
        "24 - cyan/yellow +extra",
        "25 - purple/orange +extra",
        "26 - grey/brown",
        "27 - green with eye",
        "28 - yellow/green/red",
        "29 - black/yellow",
        "30 - stars/EU",
        "31 - stars",
        "32 - EU",
        "33 - yellow/black",
        "34 - colorful",
        "35 - red/white/pink",
        "36 - blue/white/light-blue",
        "37 - Kwebbelkop",
        "38 - yellow"
    };

    private final JTextField server, name;
    private final JComboBox<String> snake;
    private final JCheckBox useRandomServer;
    private final JToggleButton connect;
    private final JLabel highscoreOTD, rank, length, kills;
    private final JSplitPane rightSplitPane, fullSplitPane;
    private final JTextArea log;
    private final JScrollBar logScrollBar;
    private final JTable highscoreList;
    private final MySlitherCanvas canvas;

    private final long startTime;
    private boolean running;
    private MySlitherWebSocketClient client;
    public final Object modelLock = new Object();

    public MySlitherJFrame() {
        super("MySlither");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) {
                    client.close();
                }
                running = false;
            }
        });

        getContentPane().setLayout(new BorderLayout());

        // === upper row ===
        JPanel settings = new JPanel(new GridBagLayout());

        server = new JTextField(16);
        server.setEnabled(false);

        name = new JTextField(16);

        snake = new JComboBox<>(SNAKES);
        snake.setMaximumRowCount(SNAKES.length);

        useRandomServer = new JCheckBox("use random server", true);
        useRandomServer.addActionListener(a -> {
            if (useRandomServer.isSelected()) {
                server.setEnabled(false);
            } else {
                server.setEnabled(true);
            }
        });

        connect = new JToggleButton("connect");
        connect.setFocusPainted(false);
        connect.addActionListener(a -> {
            if (connect.isSelected()) {
                connect.setText("disconnect");
                server.setEnabled(false);
                useRandomServer.setEnabled(false);
                name.setEnabled(false);
                snake.setEnabled(false);
                connect();
            } else {
                connect.setEnabled(false);
                connect.setText("connect");
                server.setEnabled(!useRandomServer.isSelected());
                useRandomServer.setEnabled(true);
                name.setEnabled(true);
                snake.setEnabled(true);
                disconnect();
            }
        });

        highscoreOTD = new JLabel("today's longest (???, length ???): \"???\"");

        rank = new JLabel("rank: ???/???");

        length = new JLabel("length: ???");

        kills = new JLabel("kills: ???");

        settings.add(new JLabel("server:"),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(server,
                new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("name:"),
                new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(name,
                new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("skin:"),
                new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(snake,
                new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(useRandomServer,
                new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(connect,
                new GridBagConstraints(2, 1, 1, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JSeparator(SwingConstants.VERTICAL),
                new GridBagConstraints(3, 0, 1, 3, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 6, 0, 6), 0, 0));
        settings.add(highscoreOTD,
                new GridBagConstraints(4, 0, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(rank,
                new GridBagConstraints(4, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(length,
                new GridBagConstraints(4, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(kills,
                new GridBagConstraints(5, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

        JComponent upperRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperRow.add(settings);
        getContentPane().add(upperRow, BorderLayout.NORTH);

        // === center ===
        log = new JTextArea("hi");
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(Font.decode("Monospaced 11"));
        log.setTabSize(4);
        log.getCaret().setSelectionVisible(false);
        log.getInputMap().clear();
        log.getActionMap().clear();
        log.getInputMap().put(KeyStroke.getKeyStroke("END"), "gotoEnd");
        log.getActionMap().put("gotoEnd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
                });
            }
        });
        log.getInputMap().put(KeyStroke.getKeyStroke("HOME"), "gotoStart");
        log.getActionMap().put("gotoStart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    logScrollBar.setValue(logScrollBar.getMinimum());
                });
            }
        });

        canvas = new MySlitherCanvas(this);

        highscoreList = new JTable(10, 2);
        highscoreList.setEnabled(false);
        highscoreList.getColumnModel().getColumn(0).setMinWidth(64);
        highscoreList.getColumnModel().getColumn(1).setMinWidth(192);
        highscoreList.getColumnModel().getColumn(0).setHeaderValue("length");
        highscoreList.getColumnModel().getColumn(1).setHeaderValue("name");
        highscoreList.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        highscoreList.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        highscoreList.setPreferredScrollableViewportSize(new Dimension(64 + 192, highscoreList.getPreferredSize().height));

        // == split-panes ==
        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, canvas, new JScrollPane(highscoreList));
        rightSplitPane.setDividerSize(rightSplitPane.getDividerSize() * 4 / 3);
        rightSplitPane.setResizeWeight(0.99);

        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setPreferredSize(new Dimension(300, logScrollPane.getPreferredSize().height));
        logScrollBar = logScrollPane.getVerticalScrollBar();
        fullSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, logScrollPane, rightSplitPane);
        fullSplitPane.setDividerSize(fullSplitPane.getDividerSize() * 4 / 3);
        fullSplitPane.setResizeWeight(0.1);

        getContentPane().add(fullSplitPane, BorderLayout.CENTER);

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        setSize(screenWidth * 3 / 4, screenHeight * 4 / 5);
        setLocation((screenWidth - getWidth()) / 2, (screenHeight - getHeight()) / 2);
        setExtendedState(MAXIMIZED_BOTH);

        validate();
        startTime = System.currentTimeMillis();

        running = true;
        new Thread(() -> {
            while (running) {
                long startTime = System.currentTimeMillis();
                if (client != null && client.connectionStatus == MySlitherWebSocketClient.STATUS_CONNECTED) {
                    canvas.updateModel();
                }
                canvas.repaint();
                if (client != null) {
                    client.checkForKeepalive();
                }
                try {
                    Thread.sleep(Math.max(1, 20 - (System.currentTimeMillis() - startTime)));
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    private void connect() {
        setModel(null);
        new Thread(() -> {
            if (useRandomServer.isSelected()) {
                log("fetching server-list...");
                URI[] serverList = MySlitherWebSocketClient.getServerList();
                log("received " + serverList.length + " servers");
                if (serverList.length <= 0) {
                    return;
                }

                boolean connected = false;
                while (!connected) {
                    if (client != null) {
                        client.close();
                    }
                    client = new MySlitherWebSocketClient(serverList[(int) (Math.random() * serverList.length)], this);
                    log("connecting to " + client.getURI() + " ...");
                    server.setText(client.getURI().toString());
                    try {
                        connected = client.connectBlocking();
                    } catch (InterruptedException ex) {
                        connected = false;
                    }
                }

            } else {
                if (client != null) {
                    client.close();
                }
                try {
                    client = new MySlitherWebSocketClient(new URI(server.getText()), this);
                } catch (URISyntaxException ex) {
                    log("invalid server");
                    return;
                }
                log("connecting to " + client.getURI() + " ...");
                try {
                    if (!client.connectBlocking()) {
                        log("server not reachable"); // TODO: set connect-button to connect
                        return;
                    }
                } catch (InterruptedException ex) {
                    log("interrupted while connecting");
                }
            }
            client.sendInitRequest(snake.getSelectedIndex(), name.getText());
        }).start();
    }

    private void disconnect() {
        new Thread(() -> {
            log("disconnecting...");
            try {
                client.closeBlocking();
            } catch (InterruptedException ex) {
            }
            log("disconnected");
            connect.setEnabled(true);
        }).start();
    }

    public void onClose(MySlitherWebSocketClient closedClient) {
        log("onClose");
        connect.setText("connect");
        connect.setSelected(false);
        server.setEnabled(!useRandomServer.isSelected());
        useRandomServer.setEnabled(true);
        name.setEnabled(true);
        snake.setEnabled(true);
        connect.setEnabled(true);
    }

    public void log(String text) {
        print(String.format("%6d\t%s", System.currentTimeMillis() - startTime, text));
    }

    public void print(String text) {
        SwingUtilities.invokeLater(() -> {
            boolean scrollToBottom = !logScrollBar.getValueIsAdjusting() && logScrollBar.getValue() >= logScrollBar.getMaximum() - logScrollBar.getVisibleAmount();
            log.append('\n' + text);
            fullSplitPane.getLeftComponent().validate();
            if (scrollToBottom) {
                logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
            }
        });
    }

    public void setModel(MySlitherModel model) {
        canvas.setModel(model);
    }

    public void setMap(boolean[] map) {
        canvas.setMap(map);
    }

    public void setRank(int newRank, int playerCount) {
        rank.setText("rank: " + newRank + "/" + playerCount);
    }

    public void setLength(int newLength) {
        length.setText("length: " + newLength);
    }

    public void setKills(int newKills) {
        kills.setText("kills: " + newKills);
    }

    public void setHighscoreOTD(String name, int length, String message) {
        highscoreOTD.setText("today's longest (" + name + ", length " + length + "): \"" + message + "\"");
    }

    public void setHighscoreData(int row, String name, int length, boolean highlighted) {
        highscoreList.setValueAt(highlighted ? "<html><b>" + length + "</b></html>" : length, row, 0);
        highscoreList.setValueAt(highlighted ? "<html><b>" + name + "</b></html>" : name, row, 1);
    }
}
