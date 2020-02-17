package ru.panfio.legacytester;

@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws Throwable;
}
