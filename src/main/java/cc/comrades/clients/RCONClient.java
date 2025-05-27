package cc.comrades.clients;

import cc.comrades.util.EnvLoader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class RCONClient {
    private static volatile RCONClient instance;

    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final AtomicInteger requestIdGenerator = new AtomicInteger(0);

    private RCONClient(Socket socket, DataInputStream input, DataOutputStream output) {
        this.socket = socket;
        this.input = input;
        this.output = output;
    }

    private static RCONClient create(String host, int port, String password) throws IOException {
        Socket socket = new Socket(host, port);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        RCONClient client = new RCONClient(socket, input, output);
        if (!client.authenticate(password)) {
            socket.close();
            throw new IOException("Authentication failed");
        }
        return client;
    }

    public static RCONClient getInstance() {
        if (instance == null) {
            synchronized (RCONClient.class) {
                if (instance == null) {
                    try {
                        instance = create(EnvLoader.get("RCON_HOST"), Integer.parseInt(EnvLoader.get("RCON_PORT")),
                                EnvLoader.get("RCON_PASSWORD"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return instance;
    }

    public String sendCommand(String command) throws IOException {
        int id = getNextRequestId();
        try {
            sendPacket(id, 2, command);
            RconResponse response = receivePacket();
            return response.body;
        } catch (IOException e) {
            RCONClient replacement = create(EnvLoader.get("RCON_HOST"),
                    Integer.parseInt(EnvLoader.get("RCON_PORT")),
                    EnvLoader.get("RCON_PASSWORD"));
            synchronized (RCONClient.class) {
                instance = replacement;
            }
            return instance.sendCommand(command);
        }
    }

    private void sendPacket(int id, int type, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.US_ASCII);
        int length = 4 + 4 + bodyBytes.length + 2;

        ByteBuffer buffer = ByteBuffer.allocate(4 + length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length);
        buffer.putInt(id);
        buffer.putInt(type);
        buffer.put(bodyBytes);
        buffer.put((byte) 0);
        buffer.put((byte) 0);

        output.write(buffer.array());
        output.flush();
    }

    private RconResponse receivePacket() throws IOException {
        int length = readIntLE(input);
        byte[] data = new byte[length];
        input.readFully(data);

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int id = buffer.getInt();
        int type = buffer.getInt();

        int stringLength = length - 8 - 2;
        byte[] strBytes = new byte[stringLength];
        buffer.get(strBytes);
        buffer.get();
        buffer.get();

        String body = new String(strBytes, StandardCharsets.US_ASCII);
        return new RconResponse(id, type, body);
    }

    private synchronized int getNextRequestId() {
        return requestIdGenerator.getAndIncrement();
    }

    // TODO: call on bot disconnect
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    private static class RconResponse {
        int requestId;
        int type;
        String body;

        RconResponse(int requestId, int type, String body) {
            this.requestId = requestId;
            this.type = type;
            this.body = body;
        }
    }

    private boolean authenticate(String password) throws IOException {
        int id = getNextRequestId();
        sendPacket(id, 3, password);
        RconResponse response = receivePacket();
        return response.requestId == id && response.type == 2;
    }

    private int readIntLE(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        int b4 = in.readUnsignedByte();
        return (b1) | (b2 << 8) | (b3 << 16) | (b4 << 24);
    }
}
