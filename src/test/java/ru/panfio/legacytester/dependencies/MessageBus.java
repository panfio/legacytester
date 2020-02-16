package ru.panfio.legacytester.dependencies;

import java.util.List;

public interface MessageBus {
    void send(String topic, String message);
    void send(String topic, Object message);
    <T> void sendAll(String topic, List<T> messages);
}
