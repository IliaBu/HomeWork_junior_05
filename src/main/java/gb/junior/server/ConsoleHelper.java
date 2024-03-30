package gb.junior.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message, Colors colors) {
        System.out.println(colors + message + Colors.RESET);
    }

    public static String readString() {
        while (true){
            try{
                return bufferedReader.readLine();
            } catch (IOException e) {
                writeMessage("Произошла ошибка при попытке ввода текста. Попробуйте еще раз!", Colors.RED);
            }
        }
    }
}