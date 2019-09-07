package de.mat2095.my_slither;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;


final class MySlitherJFrame extends JFrame {

    private static final String[] SNAKES = {
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
        "38 - yellow",
        "39 - PewDiePie",
        "40 - green happy",
        "41 - red with eyes",
        "42 - Google Play",
        "43 - UK",
        "44 - Ghost",
        "45 - Canada",
        "46 - Swiss",
        "47 - Moldova",
        "48 - Vietnam",
        "49 - Argentina",
        "50 - Colombia",
        "51 - Thailand",
        "52 - red/yellow",
        "53 - glowy-blue",
        "54 - glowy-red",
        "55 - glowy-yellow",
        "56 - glowy-orange",
        "57 - glowy-purple",
        "58 - glowy-green",
        "59 - yellow-M",
        "60 - detailed UK",
        "61 - glowy-colorful",
        "62 - purple spiral",
        "63 - red/black",
        "64 - blue/black"
    };

    // TODO: skins, prey-size, snake-length/width, bot-layer, that-other-thing(?), show ping

    private final JTextField server, name;
    private final JComboBox<String> snake;
    private final JCheckBox useRandomServer;
    private final JToggleButton connect;
    private final JLabel rank, kills;
    private final JSplitPane rightSplitPane, fullSplitPane;
    private final JTextArea log;
    private final JScrollBar logScrollBar;
    private final JTable highscoreList;
    private final MySlitherCanvas canvas;

    private final long startTime;
    private final Timer updateTimer;
    private Status status;
    private URI[] serverList;
    private MySlitherWebSocketClient client;
    private final Player player;
    MySlitherModel model;
    final Object modelLock = new Object();

    MySlitherJFrame() {
        super("MySlither");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                updateTimer.cancel();
                if (status == Status.CONNECTING || status == Status.CONNECTED) {
                    disconnect();
                }
                canvas.repaintThread.shutdown();
                try {
                    canvas.repaintThread.awaitTermination(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        getContentPane().setLayout(new BorderLayout());

        canvas = new MySlitherCanvas(this);
        player = canvas.mouseInput;

        // === upper row ===
        JPanel settings = new JPanel(new GridBagLayout());

        server = new JTextField(18);

        name = new JTextField("MySlitherEaterBot", 16);

        snake = new JComboBox<>(SNAKES);
        snake.setMaximumRowCount(snake.getItemCount());

        useRandomServer = new JCheckBox("use random server", true);
        useRandomServer.addActionListener(a -> {
            setStatus(null);
        });

        connect = new JToggleButton();
        connect.addActionListener(a -> {
            switch (status) {
                case DISCONNECTED:
                    connect();
                    break;
                case CONNECTING:
                case CONNECTED:
                    disconnect();
                    break;
                case DISCONNECTING:
                    break;
            }
        });

        rank = new JLabel();

        kills = new JLabel();

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
        settings.add(new JLabel("kills:"),
            new GridBagConstraints(4, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(kills,
            new GridBagConstraints(5, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("rank:"),
            new GridBagConstraints(4, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(rank,
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
        setStatus(Status.DISCONNECTED);

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (modelLock) {
                    if (status == Status.CONNECTED && model != null) {
                        model.update();
                        client.sendData(player.action(model));
                    }
                }
            }
        }, 1, 10);
    }

    void onOpen() {
        switch (status) {
            case CONNECTING:
                setStatus(Status.CONNECTED);
                client.sendInitRequest(snake.getSelectedIndex(), name.getText());
                break;
            case DISCONNECTING:
                disconnect();
                break;
            default:
                throw new IllegalStateException("Connected while not connecting!");
        }
    }

    void onClose() {
        switch (status) {
            case CONNECTED:
            case DISCONNECTING:
                setStatus(Status.DISCONNECTED);
                client = null;
                break;
            case CONNECTING:
                client = null;
                trySingleConnect();
                break;
            default:
                throw new IllegalStateException("Disconnected while not connecting, connected or disconnecting!");
        }
    }

    private void connect() {
        new Thread(() -> {
            if (status != Status.DISCONNECTED) {
                throw new IllegalStateException("Connecting while not disconnected");
            }
            setStatus(Status.CONNECTING);
            setModel(null);

            if (useRandomServer.isSelected()) {
                log("fetching server-list...");
                serverList = MySlitherWebSocketClient.getServerList();
                log("received " + serverList.length + " servers");
                if (serverList.length <= 0) {
                    log("no server found");
                    setStatus(Status.DISCONNECTED);
                    return;
                }
            }

            if (status == Status.CONNECTING) {
                trySingleConnect();
            }
        }).start();
    }

    private void trySingleConnect() {
        if (status != Status.CONNECTING) {
            throw new IllegalStateException("Trying single connection while not connecting");
        }

        if (useRandomServer.isSelected()) {
            client = new MySlitherWebSocketClient(serverList[(int) (Math.random() * serverList.length)], this);
            server.setText(client.getURI().toString());
        } else {
            try {
                client = new MySlitherWebSocketClient(new URI(server.getText()), this);
            } catch (URISyntaxException ex) {
                log("invalid server");
                setStatus(Status.DISCONNECTED);
                return;
            }
        }

        log("connecting to " + client.getURI() + " ...");
        client.connect();
    }

    private void disconnect() {
        if (status == Status.DISCONNECTED) {
            throw new IllegalStateException("Already disconnected");
        }
        setStatus(Status.DISCONNECTING);
        if (client != null) {
            client.close();
        }
    }

    private void setStatus(Status newStatus) {
        if (newStatus != null) {
            status = newStatus;
        }
        connect.setText(status.buttonText);
        connect.setSelected(status.buttonSelected);
        connect.setEnabled(status.buttonEnabled);
        server.setEnabled(status.allowModifyData && !useRandomServer.isSelected());
        useRandomServer.setEnabled(status.allowModifyData);
        name.setEnabled(status.allowModifyData);
        snake.setEnabled(status.allowModifyData);
    }

    void log(String text) {
        print(String.format("%6d\t%s", System.currentTimeMillis() - startTime, text));
    }

    private void print(String text) {
        SwingUtilities.invokeLater(() -> {
            boolean scrollToBottom = !logScrollBar.getValueIsAdjusting() && logScrollBar.getValue() >= logScrollBar.getMaximum() - logScrollBar.getVisibleAmount();
            log.append('\n' + text);
            fullSplitPane.getLeftComponent().validate();
            if (scrollToBottom) {
                logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
            }
        });
    }

    void setModel(MySlitherModel model) {
        synchronized (modelLock) {
            this.model = model;
            rank.setText(null);
            kills.setText(null);
        }
    }

    void setMap(boolean[] map) {
        canvas.setMap(map);
    }

    void setRank(int newRank, int playerCount) {
        rank.setText(newRank + "/" + playerCount);
    }

    void setKills(int newKills) {
        kills.setText(String.valueOf(newKills));
    }

    void setHighscoreData(int row, String name, int length, boolean highlighted) {
        highscoreList.setValueAt(highlighted ? "<html><b>" + length + "</b></html>" : length, row, 0);
        highscoreList.setValueAt(highlighted ? "<html><b>" + name + "</b></html>" : name, row, 1);
    }

    private enum Status {
        DISCONNECTED("connect", false, true, true),
        CONNECTING("connecting...", true, true, false),
        CONNECTED("disconnect", true, true, false),
        DISCONNECTING("disconnecting...", false, false, false);

        private final String buttonText;
        private final boolean buttonSelected, buttonEnabled;
        private final boolean allowModifyData;

        private Status(String buttonText, boolean buttonSelected, boolean buttonEnabled, boolean allowModifyData) {
            this.buttonText = buttonText;
            this.buttonSelected = buttonSelected;
            this.buttonEnabled = buttonEnabled;
            this.allowModifyData = allowModifyData;
        }
    }
}
