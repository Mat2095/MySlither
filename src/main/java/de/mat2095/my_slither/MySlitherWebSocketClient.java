package de.mat2095.my_slither;

import static de.mat2095.my_slither.MySlitherModel.PI2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;


final class MySlitherWebSocketClient extends WebSocketClient {

    private static final Map<String, String> HEADER = new LinkedHashMap<>();
    private static final byte[] DATA_PING = new byte[]{(byte) 251};
    private static final byte[] DATA_BOOST_START = new byte[]{(byte) 253};
    private static final byte[] DATA_BOOST_STOP = new byte[]{(byte) 254};

    private final MySlitherJFrame view;
    private MySlitherModel model;

    private byte[] initRequest;
    private long lastAngleTime, lastPingTime;
    private byte lastAngleContent, angleToBeSent;
    private boolean lastBoostContent;
    private boolean waitingForPong;

    static {
        HEADER.put("Origin", "http://slither.io");
        HEADER.put("Pragma", "no-cache");
        HEADER.put("Cache-Control", "no-cache");
    }

    MySlitherWebSocketClient(URI serverUri, MySlitherJFrame view) {
        super(serverUri, new Draft_6455(), HEADER);
        this.view = view;
    }

    void sendData(Player.Wish wish) {

        if (wish.angle != null) {
            angleToBeSent = (byte) (wish.angle * 251 / PI2);
        }
        if (angleToBeSent != lastAngleContent && System.currentTimeMillis() - lastAngleTime > 100) {
            lastAngleTime = System.currentTimeMillis();
            lastAngleContent = angleToBeSent;
            send(new byte[]{angleToBeSent});
        }

        if (wish.boost != null && wish.boost != lastBoostContent) {
            lastBoostContent = wish.boost;
            send(wish.boost ? DATA_BOOST_START : DATA_BOOST_STOP);
        }

        if (!waitingForPong && System.currentTimeMillis() - lastPingTime > 250) {
            lastPingTime = System.currentTimeMillis();
            waitingForPong = true;
            send(DATA_PING);
        }
    }

    @Override
    public void onOpen(ServerHandshake sh) {
        view.log("connected: " + sh.getHttpStatusMessage());
        view.onOpen();
    }

    @Override
    public void onClose(int i, String string, boolean bln) {
        view.log("closed: " + i + ", " + bln + ", " + string);
        view.onClose();
    }

    @Override
    public void onMessage(String string) {
        view.log("message: " + string);
    }

    @Override
    public void onError(Exception ex) {
        view.log("ERROR: " + ex);
        ex.printStackTrace();
    }

    @Override
    public void onMessage(ByteBuffer bytes) { // TODO: use first two bytes
        byte[] b = bytes.array();
        if (b.length < 3) {
            view.log("too short");
            return;
        }
        int[] data = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            data[i] = b[i] & 0xFF;
        }
        char cmd = (char) data[2];
        switch (cmd) {
            case '6':
                processPreInitResponse(data);
                break;
            case 'a':
                processInitResponse(data);
                break;
            case 'e':
            case 'E':
            case '3':
            case '4':
            case '5':
                processUpdateBodyparts(data, cmd);
                break;
            case 'h':
                processUpdateFam(data);
                break;
            case 'r':
                processRemoveSnakePart(data);
                break;
            case 'g':
            case 'n':
            case 'G':
            case 'N':
                processUpdateSnakePosition(data, cmd);
                break;
            case 'l':
                processLeaderboard(data);
                break;
            case 'v':
                processDead(data);
                break;
            case 'W':
                processAddSector(data);
                break;
            case 'w':
                processRemoveSector(data);
                break;
            case 'm':
                processGlobalHighscore(data);
                break;
            case 'p':
                processPong(data);
                break;
            case 'u':
                processUpdateMinimap(data);
                break;
            case 's':
                processAddRemoveSnake(data);
                break;
            case 'b':
            case 'f':
            case 'F':
                processAddFood(data, cmd == 'F', cmd != 'f');
                break;
            case 'c':
                processRemoveFood(data);
                break;
            case 'j':
                processUpdatePrey(data);
                break;
            case 'y':
                processAddRemovePrey(data);
                break;
            case 'k':
                processKill(data);
                break;
            default:
                view.log("Unknown command: " + cmd);
        }
    }

    private void processPreInitResponse(int[] data) {
        view.log("sending decrypted, manipulated secret");
        send(decodeSecret(data));

        view.log("sending init-request");
        send(initRequest);
    }

    private static byte[] decodeSecret(int[] secret) {

        byte[] result = new byte[24];

        int globalValue = 0;
        for (int i = 0; i < 24; i++) {
            int value1 = secret[17 + i * 2];
            if (value1 <= 96) {
                value1 += 32;
            }
            value1 = (value1 - 98 - i * 34) % 26;
            if (value1 < 0) {
                value1 += 26;
            }

            int value2 = secret[18 + i * 2];
            if (value2 <= 96) {
                value2 += 32;
            }
            value2 = (value2 - 115 - i * 34) % 26;
            if (value2 < 0) {
                value2 += 26;
            }

            int interimResult = (value1 << 4) | value2;
            int offset = interimResult >= 97 ? 97 : 65;
            interimResult -= offset;
            if (i == 0) {
                globalValue = 2 + interimResult;
            }
            result[i] = (byte) ((interimResult + globalValue) % 26 + offset);
            globalValue += 3 + interimResult;
        }

        return result;
    }

    private void processInitResponse(int[] data) {
        if (data.length != 26) {
            view.log("init response wrong length!");
            return;
        }

        int gameRadius = (data[3] << 16) | (data[4] << 8) | data[5];
        int mscps = (data[6] << 8) | data[7];
        int sectorSize = (data[8] << 8) | data[9];
        double spangdv = data[12] / 10.0;
        double nsp1 = ((data[13] << 8) | data[14]) / 100.0;
        double nsp2 = ((data[15] << 8) | data[16]) / 100.0;
        double nsp3 = ((data[17] << 8) | data[18]) / 100.0;
        double mamu1 = ((data[19] << 8) | data[20]) / 1000.0;
        double mamu2 = ((data[21] << 8) | data[22]) / 1000.0; // prey angle speed
        double cst = ((data[23] << 8) | data[24]) / 1000.0;
        int protocolVersion = data[25];

        if (protocolVersion != 11) {
            view.log("wrong protocol-version (" + protocolVersion + ")");
            close();
            return;
        }

        model = new MySlitherModel(gameRadius, sectorSize, spangdv, nsp1, nsp2, nsp3, mamu1, mamu2, cst, mscps, view);
        view.setModel(model);
        view.setKills(0);
    }

    private void processUpdateBodyparts(int[] data, char cmd) {
        if (data.length != 8 && data.length != 7 && data.length != 6) {
            view.log("update body-parts wrong length!");
            return;
        }

        int snakeID = (data[3] << 8) | data[4];
        int newDir = -1;
        double newAng = -1;
        double newWang = -1;
        double newSpeed = -1;
        if (data.length == 8) {
            newDir = cmd == 'e' ? 1 : 2;
            newAng = data[5] * PI2 / 256;
            newWang = data[6] * PI2 / 256;
            newSpeed = data[7] / 18.0;
        } else if (data.length == 7) {
            switch (cmd) {
                case 'e':
                    newAng = data[5] * PI2 / 256;
                    newSpeed = data[6] / 18.0;
                    break;
                case 'E':
                    newDir = 1;
                    newWang = data[5] * PI2 / 256;
                    newSpeed = data[6] / 18.0;
                    break;
                case '4':
                    newDir = 2;
                    newWang = data[5] * PI2 / 256;
                    newSpeed = data[6] / 18.0;
                    break;
                case '3':
                    newDir = 1;
                    newAng = data[5] * PI2 / 256;
                    newWang = data[6] * PI2 / 256;
                    break;
                case '5':
                    newDir = 2;
                    newAng = data[5] * PI2 / 256;
                    newWang = data[6] * PI2 / 256;
                    break;
                default:
                    view.log("update body-parts invalid cmd/length: " + cmd + ", " + data.length);
                    return;
            }
        } else if (data.length == 6) {
            switch (cmd) {
                case 'e':
                    newAng = data[5] * PI2 / 256;
                    break;
                case 'E':
                    newDir = 1;
                    newWang = data[5] * PI2 / 256;
                    break;
                case '4':
                    newDir = 2;
                    newWang = data[5] * PI2 / 256;
                    break;
                case '3':
                    newSpeed = data[5] / 18.0;
                    break;
                default:
                    view.log("update body-parts invalid cmd/length: " + cmd + ", " + data.length);
                    return;
            }
        }

        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeID);
            if (newDir != -1) {
                snake.dir = newDir;
            }
            if (newAng != -1) {
                snake.ang = newAng;
            }
            if (newWang != -1) {
                snake.wang = newWang;
            }
            if (newSpeed != -1) {
                snake.sp = newSpeed;
            }
        }
    }

    private void processUpdateFam(int[] data) {
        if (data.length != 8) {
            view.log("update fam wrong length!");
            return;
        }
        int snakeID = (data[3] << 8) | data[4];
        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeID);
            snake.setFam(((data[5] << 16) | (data[6] << 8) | (data[7])) / 16777215.0);
        }
    }

    private void processRemoveSnakePart(int[] data) {
        if (data.length != 5 && data.length != 8) {
            view.log("remove snake part wrong length!");
        }
        int snakeID = (data[3] << 8) | data[4];
        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeID);
            if (data.length == 8) {
                snake.setFam(((data[5] << 16) | (data[6] << 8) | (data[7])) / 16777215.0);
            }
            snake.body.pollLast();
        }
    }

    private void processUpdateSnakePosition(int[] data, char cmd) {

        boolean absoluteCoords = cmd == 'g' || cmd == 'n';
        boolean newBodyPart = cmd == 'n' || cmd == 'N';

        if (data.length != 5 + (absoluteCoords ? 4 : 2) + (newBodyPart ? 3 : 0)) {
            view.log("update snake body wrong length!");
            return;
        }

        int snakeID = (data[3] << 8) | data[4];

        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeID);
            SnakeBodyPart head = snake.body.getFirst();

            double newX = absoluteCoords ? (data[5] << 8) | data[6] : head.x + data[5] - 128;
            double newY = absoluteCoords ? (data[7] << 8) | data[8] : head.y + data[6] - 128;

            if (newBodyPart) {
                snake.setFam(((data[absoluteCoords ? 9 : 7] << 16) | (data[absoluteCoords ? 10 : 8] << 8) | (data[absoluteCoords ? 11 : 9])) / 16777215.0);
            } else {
                snake.body.pollLast();
            }

            snake.body.addFirst(new SnakeBodyPart(newX, newY));

            snake.x = newX;
            snake.y = newY;
        }
    }

    private void processLeaderboard(int[] data) {
        if (data.length < 8 + 10 * 7) {
            view.log("leaderboard wrong length!");
            return;
        }

        int ownRank = (data[4] << 8) | data[5];
        int playerCount = (data[6] << 8) | data[7];

        view.setRank(ownRank, playerCount);

        int rank = 0;
        int cursorPosition = 8;
        while (cursorPosition + 6 < data.length) {
            int bodyLength = (data[cursorPosition] << 8) | data[cursorPosition + 1];
            double fillAnount = ((data[cursorPosition + 2] << 16) | (data[cursorPosition + 3] << 8) | (data[cursorPosition + 4])) / 16777215.0;
            int nameLength = data[cursorPosition + 6];
            StringBuilder name = new StringBuilder(nameLength);
            for (int i = 0; i < nameLength && cursorPosition + 7 + i < data.length; i++) {
                name.append((char) data[cursorPosition + 7 + i]);
            }
            cursorPosition += 7 + nameLength;
            rank++;
            view.setHighscoreData(rank - 1, name.toString(), model.getSnakeLength(bodyLength, fillAnount), ownRank == rank);
        }
    }

    private void processDead(int[] data) {
        if (data.length != 4) {
            view.log("dead wrong length!");
            return;
        }
        int deathReason = data[3];
        switch (deathReason) {
            case 0:
                view.log("You died.");
                break;
            case 1:
                view.log("You've achieved a new record!");
                break;
            case 2:
                view.log("Death reason 2, unknown");
                break;
            default:
                view.log("invalid death reason: " + deathReason + "!");
        }
    }

    private void processAddSector(int[] data) {
        if (data.length != 5) {
            view.log("add sector wrong length!");
            return;
        }

        int sectorX = data[3];
        int sectorY = data[4];

        model.addSector(sectorX, sectorY);
    }

    private void processRemoveSector(int[] data) {
        if (data.length != 5) {
            view.log("remove sector wrong length!");
            return;
        }

        int sectorX = data[3];
        int sectorY = data[4];

        model.removeSector(sectorX, sectorY);
    }

    private void processGlobalHighscore(int[] data) {
        if (data.length < 10) {
            view.log("global highscore wrong length");
            return;
        }

        int bodyLength = (data[3] << 16) | (data[4] << 8) | (data[5]);
        double fillAmount = ((data[6] << 16) | (data[7] << 8) | (data[8])) / 16777215.0;
        int nameLength = data[9];
        StringBuilder name = new StringBuilder(nameLength);
        for (int i = 0; i < nameLength; i++) {
            name.append((char) data[10 + i]);
        }
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < data.length - 10 - nameLength; i++) {
            message.append((char) data[10 + nameLength + i]);
        }
        view.log("Received Highscore of the day: " + name.toString() + " (" + model.getSnakeLength(bodyLength, fillAmount) + "): " + message.toString());
    }

    private void processPong(int[] data) {
        if (data.length != 3) {
            view.log("pong wrong length!");
            return;
        }

        waitingForPong = false;
    }

    private void processUpdateMinimap(int[] data) {
        boolean[] map = new boolean[80 * 80];
        int mapPos = 0;
        for (int dataPos = 3; dataPos < data.length; dataPos++) {
            int value = data[dataPos];
            if (value >= 128) {
                value -= 128;
                mapPos += value;
                if (mapPos >= map.length) {
                    break;
                }
            } else {
                for (int i = 0; i < 7; i++) {
                    if ((value & (1 << (6 - i))) != 0) {
                        map[mapPos] = true;
                    }
                    mapPos++;
                    if (mapPos >= map.length) {
                        break;
                    }
                }
            }
        }
        view.setMap(map);
    }

    private void processAddRemoveSnake(int[] data) {
        if (data.length >= 31) {
            int id = (data[3] << 8) | (data[4]);

            double ang = ((data[5] << 16) | (data[6] << 8) | data[7]) * PI2 / 16777215.0;
            double wang = ((data[9] << 16) | (data[10] << 8) | data[11]) * PI2 / 16777215.0;

            double sp = ((data[12] << 8) | data[13]) / 1000.0;
            double fam = ((data[14] << 16) | (data[15] << 8) | data[16]) / 16777215.0;

            double x = ((data[18] << 16) | (data[19] << 8) | data[20]) / 5.0;
            double y = ((data[21] << 16) | (data[22] << 8) | data[23]) / 5.0;

            int nameLength = data[24];
            StringBuilder name = new StringBuilder(nameLength);
            for (int i = 0; i < nameLength; i++) {
                name.append((char) data[25 + i]);
            }

            int customSkinDataLength = data[nameLength + 25];

            double currentBodyPartX = ((data[nameLength + customSkinDataLength + 26] << 16) | (data[nameLength + customSkinDataLength + 27] << 8) | data[nameLength + customSkinDataLength + 28]) / 5.0;
            double currentBodyPartY = ((data[nameLength + customSkinDataLength + 29] << 16) | (data[nameLength + customSkinDataLength + 30] << 8) | data[nameLength + customSkinDataLength + 31]) / 5.0;

            Deque<SnakeBodyPart> body = new LinkedList<>();
            body.addFirst(new SnakeBodyPart(currentBodyPartX, currentBodyPartY));

            for (int nextBodyPartStartPosition = nameLength + customSkinDataLength + 32; nextBodyPartStartPosition + 1 < data.length; nextBodyPartStartPosition += 2) {
                currentBodyPartX += (data[nextBodyPartStartPosition] - 127) / 2.0;
                currentBodyPartY += (data[nextBodyPartStartPosition + 1] - 127) / 2.0;
                body.addFirst(new SnakeBodyPart(currentBodyPartX, currentBodyPartY));
            }

            model.addSnake(id, name.toString(), x, y, wang, ang, sp, fam, body);
        } else if (data.length == 6) {
            int id = (data[3] << 8) | (data[4]);
            model.removeSnake(id);
        } else {
            view.log("add/remove snake wrong length!");
        }
    }

    private void processAddFood(int[] data, boolean allowMultipleEntities, boolean fastSpawn) {
        if ((!allowMultipleEntities && data.length != 9) || (allowMultipleEntities && (data.length < 9 || ((data.length - 9) % 6 != 0)))) {
            view.log("add food wrong length!");
            return;
        }
        for (int i = 8; i < data.length; i += 6) {
            int x = (data[i - 4] << 8) | data[i - 3];
            int z = (data[i - 2] << 8) | data[i - 1];
            double radius = data[i] / 5.0;
            model.addFood(x, z, radius, fastSpawn); // TODO: now always...
        }
    }

    private void processRemoveFood(int[] data) {
        if (data.length != 7 && data.length != 9) {
            view.log("remove food wrong length!");
            return;
        }

        int x = (data[3] << 8) | data[4];
        int y = (data[5] << 8) | data[6];

        model.removeFood(x, y);
    }

    private void processUpdatePrey(int[] data) {
        if (data.length != 11 && data.length != 12 && data.length != 13 && data.length != 14 && data.length != 15 && data.length != 16 && data.length != 18) {
            view.log("update prey wrong length!");
            return;
        }

        int id = (data[3] << 8) | data[4];
        int x = 1 + 3 * ((data[5] << 8) | data[6]);
        int y = 1 + 3 * ((data[7] << 8) | data[8]);

        synchronized (view.modelLock) {
            Prey prey = model.getPrey(id);
            prey.x = x;
            prey.y = y;

            switch (data.length) {
                case 11:
                    prey.sp = ((data[9] << 8) | data[10]) / 1000.0;
                    break;
                case 12:
                    prey.ang = ((data[9] << 16) | (data[10] << 8) | data[11]) * PI2 / 16777215;
                    break;
                case 13:
                    prey.dir = data[9] - 48;
                    prey.wang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / 16777215;
                    break;
                case 14:
                    prey.ang = ((data[9] << 16) | (data[10] << 8) | data[11]) * PI2 / 16777215;
                    prey.sp = ((data[12] << 8) | data[13]) / 1000.0;
                    break;
                case 15:
                    prey.dir = data[9] - 48;
                    prey.wang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / 16777215;
                    prey.sp = ((data[13] << 8) | data[14]) / 1000.0;
                    break;
                case 16:
                    prey.dir = data[9] - 48;
                    prey.ang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / 16777215;
                    prey.wang = ((data[13] << 16) | (data[14] << 8) | data[15]) * PI2 / 16777215;
                    break;
                case 18:
                    prey.dir = data[9] - 48;
                    prey.ang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / 16777215;
                    prey.wang = ((data[13] << 16) | (data[14] << 8) | data[15]) * PI2 / 16777215;
                    prey.sp = ((data[16] << 8) | data[17]) / 1000.0;
                    break;
            }
        }
    }

    private void processAddRemovePrey(int[] data) {
        if (data.length == 7) {
            int id = (data[3] << 8) | data[4];
            int eaterID = (data[5] << 8) | data[6];
            model.removePrey(id);
        } else if (data.length == 5) {
            int id = (data[3] << 8) | data[4];
            model.removePrey(id);
        } else if (data.length == 22) {
            int id = (data[3] << 8) | data[4];
            double x = ((data[6] << 16) | (data[7] << 8) | data[8]) / 5.0;
            double y = ((data[9] << 16) | (data[10] << 8) | data[11]) / 5.0;
            double radius = data[12] / 5.0;
            int dir = data[13] - 48;
            double wang = ((data[14] << 16) | (data[15] << 8) | data[16]) * PI2 / 16777215;
            double ang = ((data[17] << 16) | (data[18] << 8) | data[19]) * PI2 / 16777215;
            double sp = ((data[20] << 8) | data[21]) / 1000.0;
            model.addPrey(id, x, y, radius, dir, wang, ang, sp);
        } else {
            view.log("add/remove prey wrong length!");
        }
    }

    private void processKill(int[] data) {
        if (data.length != 8) {
            view.log("kill wrong length!");
            return;
        }

        int id = (data[3] << 8) | data[4];
        int kills = (data[5] << 16) | (data[6] << 8) | data[7];

        if (id == model.snake.id) {
            view.setKills(kills);
        } else {
            view.log("kill packet with invalid id: " + id);
        }
    }

    void sendInitRequest(int snakeNr, String nick) {

        initRequest = new byte[4 + nick.length()];
        initRequest[0] = 115;
        initRequest[1] = 10;
        initRequest[2] = (byte) snakeNr;
        initRequest[3] = (byte) nick.length();
        for (int i = 0; i < nick.length(); i++) {
            initRequest[4 + i] = (byte) nick.codePointAt(i);
        }

        // pre-init request
        view.log("sending pre-init request");
        send(new byte[]{99});
    }

    static URI[] getServerList() {

        String i49526_String;
        try {
            HttpURLConnection i49526_HttpURLConnection = (HttpURLConnection) new URL("http://slither.io/i33628.txt").openConnection();
            i49526_HttpURLConnection.setRequestProperty("User-Agent", "java/1.8.0_72");
            InputStream i49526_InputStream = i49526_HttpURLConnection.getInputStream();
            BufferedReader i49526_BufferedReader = new BufferedReader(new InputStreamReader(i49526_InputStream));
            i49526_String = i49526_BufferedReader.lines().collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            throw new Error("Error reading server-list!"); // TODO: set to disconnected
        }

        int[] data = new int[(i49526_String.length() - 1) / 2];
        for (int i = 0; i < data.length; i++) {
            int u1 = (i49526_String.codePointAt(i * 2 + 1) - 97 - 14 * i) % 26;
            if (u1 < 0) {
                u1 += 26;
            }
            int u2 = (i49526_String.codePointAt(i * 2 + 2) - 104 - 14 * i) % 26;
            if (u2 < 0) {
                u2 += 26;
            }
            data[i] = (u1 << 4) + u2;
        }

        URI[] serverList = new URI[(i49526_String.length() - 1) / 22];
        for (int i = 0; i < serverList.length; i++) {
            try {
                serverList[i] = new URI("ws://"
                    + data[i * 11 + 0] + "."
                    + data[i * 11 + 1] + "."
                    + data[i * 11 + 2] + "."
                    + data[i * 11 + 3] + ":"
                    + ((data[i * 11 + 4] << 16) + (data[i * 11 + 5] << 8) + data[i * 11 + 6])
                    + "/slither");
            } catch (URISyntaxException ex) {
                throw new Error("Error building server-address!");
            }
        }

        return serverList;
    }
}
