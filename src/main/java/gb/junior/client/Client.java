package gb.junior.client;

import gb.junior.server.*;
import java.io.IOException;
import java.net.Socket;

import static gb.junior.server.Server.*;

public class Client {
    protected Connection connection;

    private volatile boolean clientConnected = false;

    protected String getServerAddress() {
        return address;
    }

    protected int getServerPort() {
        return port;
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException ioException) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Ошибка во время отправки сообщения", Colors.RED);
        }
    }

    protected void sendLeftMessage() {
        try {
            Message message = new Message(MessageType.USER_EXIT, " покинул чат");
            connection.send(message);
        } catch (IOException ioException) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Ошибка во время отправки сообщения", Colors.RED);
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Ошибка во время ожидания потоком: " + socketThread.getName(), Colors.RED);
            return;
        }
        if (clientConnected)
            ConsoleHelper.writeMessage("""
                    Соединение установлено.
                    Для выхода наберите команду '/exit'""", Colors.MAGENTA);
        else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента", Colors.RED);
        }
        while (clientConnected) {
            String stringMessage = ConsoleHelper.readString();
            if (stringMessage.equalsIgnoreCase("/exit")) {
                sendLeftMessage();
                break;
            }
            if (shouldSendTextFromConsole())
                sendTextMessage(stringMessage);
        }
    }

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message, Colors.RED);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " присоединился к чату", Colors.GREEN);
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " покинул чат", Colors.BLUE);
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            synchronized (Client.this) {
                Client.this.clientConnected = clientConnected;
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message receivedMessage = connection.receive();
                MessageType typeOfReceivedMessage = receivedMessage.getType();
                if (typeOfReceivedMessage == null)
                    ConsoleHelper.writeMessage("Неожиданный тип сообщения", Colors.RED);
                switch (typeOfReceivedMessage) {
                    case NAME_REQUEST -> {
                        ConsoleHelper.writeMessage("Введите своё имя: ", Colors.YELLOW);
                        Message myNameForServerMessage = new Message(MessageType.USER_NAME, getUserName());
                        connection.send(myNameForServerMessage);
                    }
                    case NAME_ACCEPTED -> {
                        notifyConnectionStatusChanged(true);
                        return;
                    }
                    default -> ConsoleHelper.writeMessage("Неожиданный тип сообщения", Colors.RED);
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message receivedMessage = connection.receive();
                MessageType typeOfReceivedMessage = receivedMessage.getType();
                if (typeOfReceivedMessage == null)
                    ConsoleHelper.writeMessage("Неожиданный тип сообщения", Colors.RED);
                switch (typeOfReceivedMessage) {
                    case TEXT -> {
                        String dataOfReceivedMessage = receivedMessage.getData();
                        processIncomingMessage(dataOfReceivedMessage);
                    }
                    case USER_ADDED -> {
                        String newUserName = receivedMessage.getData();
                        informAboutAddingNewUser(newUserName);
                    }
                    case USER_REMOVED -> {
                        String removedUserName = receivedMessage.getData();
                        informAboutDeletingNewUser(removedUserName);
                    }
                    default -> ConsoleHelper.writeMessage("Неожиданный тип сообщения", Colors.RED);
                }
            }
        }

        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try(Socket socket = new Socket(serverAddress, serverPort)
            ) {
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
                ConsoleHelper.writeMessage(e.getMessage(), Colors.RED);
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
