package ru.panfio.legacytester.constructor;

public interface TestConstructor {
    String construct();
    TestConstructor configuration(ConstructorConfiguration conf);
}