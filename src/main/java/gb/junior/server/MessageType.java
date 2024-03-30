package gb.junior.server;

public enum MessageType {
    NAME_REQUEST,   //запрос имени
    USER_NAME,      //имя пользователя
    NAME_ACCEPTED,  //имя принято
    TEXT,           //текстовое сообщение
    USER_ADDED,     //пользователь добавлен
    USER_REMOVED,   //пользователь удален
    USER_EXIT       //пользователь покинул чат, когда написал exit
}