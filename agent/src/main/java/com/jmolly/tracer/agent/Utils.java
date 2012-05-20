package com.jmolly.tracer.agent;

public class Utils {

    private Utils() {}

    public static String toJavaName(String className) {
        return className.replaceAll("/", ".");
    }

    public static void log(String msg) {
        System.out.println("[tracer-agent] " + msg);
    }

    public static void assertFalse(boolean value) {
        if (value) {
            throw new IllegalStateException();
        }
    }

}
