package gb.junior.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    public static int port = 8181;
    public static String address = "localhost";

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Map.Entry<String, Connection> pair : connectionMap.entrySet()) {
                pair.getValue().send(message);
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Не удалось отправить сообщение " + e.getMessage(), Colors.RED);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage(String.format("Сервер запущен.\nIP: %s\nport: %d", address, port), Colors.MAGENTA);
            while (true) {
                Socket socket = serverSocket.accept();
                ServerHandler handler = new ServerHandler(socket);
                handler.start();
                continue;
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Cерверный сокет закрыт из за возникновения ошибки: " + e.getMessage(), Colors.RED);
        }
    }


}