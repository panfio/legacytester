package ru.panfio.legacytester.util;

import java.io.*;
import java.util.Base64;

public final class SerializableUtils {
    private SerializableUtils() {
        throw new RuntimeException("Utility class");
    }

    /**
     * Serialize the object from Base64 string.
     */
    public static Object serializeFromString(String s) {
        try {
            byte[] data = Base64.getDecoder().decode(s);
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Serialize the object to a Base64 string.
     */
    public static String serializeToString(Serializable object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
