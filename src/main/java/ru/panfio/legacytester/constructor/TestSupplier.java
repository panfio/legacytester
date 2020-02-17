package ru.panfio.legacytester.constructor;

@FunctionalInterface
public interface TestSupplier<T, U, R> {
    R get(T t, U u);
}