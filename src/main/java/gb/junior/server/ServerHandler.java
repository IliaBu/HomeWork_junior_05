package gb.junior.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import static gb.junior.server.Server.*;

public class ServerHandler extends Thread{
    private final Socket socket;

    private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
        while(true){
            connection.send(new Message(MessageType.NAME_REQUEST));
            Message answer = connection.receive();
            MessageType type = answer.getType();
            if (type != MessageType.USER_NAME)
                continue;
            String name = answer.getData();
            if (name == null)
                continue;
            if (name.equals(""))
                continue;
            if (connectionMap.containsKey(name))
                continue;
            connectionMap.put(name, connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            return name;
        }
    }

    private void notifyUsers(Connection connection, String userName) throws IOException {
        for (Map.Entry<String, Connection> pair :
                connectionMap.entrySet()) {
            String name = pair.getKey();
            if (!name.equals(userName)){
                Message message = new Message(MessageType.USER_ADDED, name);
                connection.send(message);
            }

        }
    }

    private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
        while (true) {
            Message message = connection.receive();
            MessageType type = message.getType();
            if (type == MessageType.USER_EXIT)
                break;

            if (type != MessageType.TEXT) {
                ConsoleHelper.writeMessage(String.format("Тип сообщения %s не совпадает с %s", type, MessageType.TEXT), Colors.RED);
                continue;
            }
            String messageToAllUsersData = userName + ": " + message.getData();
            Message messageToAllUsers = new Message(MessageType.TEXT, messageToAllUsersData);
            sendBroadcastMessage(messageToAllUsers);
        }
    }

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        ConsoleHelper.writeMessage("Установлено новое соединение с адресом: " + socket.getLocalSocketAddress(), Colors.GREEN);
        try(Connection connection = new Connection(socket))  {
            String newUserName = serverHandshake(connection);
            Message newUserAddedMessage = new Message(MessageType.USER_ADDED, newUserName);
            sendBroadcastMessage(newUserAddedMessage);
            notifyUsers(connection, newUserName);
            serverMainLoop(connection, newUserName);
            if (newUserName != null) {
                connectionMap.remove(newUserName);
                Message newUserRemovedMessage = new Message(MessageType.USER_REMOVED, newUserName);
                sendBroadcastMessage(newUserRemovedMessage);
            }
            ConsoleHelper.writeMessage("Соединение с адресом " + socket.getLocalSocketAddress() + " закрыто", Colors.GREEN);
        } catch (IOException | ClassNotFoundException ioException) {
            ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с адресом", Colors.RED);
        }
    }
}