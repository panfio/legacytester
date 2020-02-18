package ru.panfio.legacytester.constructor;

@FunctionalInterface
public interface ConstructorSupplier<T, U, C, R> {
    R get(T t, U u, C c);
}