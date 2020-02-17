package ru.panfio.legacytester;

@FunctionalInterface
public interface ThrowableRunnable {
    void run() throws Throwable;
}
