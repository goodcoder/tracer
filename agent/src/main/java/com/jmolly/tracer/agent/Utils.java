package com.jmolly.tracer.agent;

public final class Utils {

    public static volatile boolean logEnabled = false;

    private Utils() {}

    public static String toJavaName(String className) {
        return className.replaceAll("/", ".");
    }

    public static void log(String msg) {
        if (logEnabled) {
            System.out.println("[tracer-agent] " + msg);
        }
    }

    public static void log(Exception e) {
        if (logEnabled) {
            e.printStackTrace(System.out);
        }
    }

    public static void assertFalse(boolean value) {
        if (value) {
            throw new IllegalStateException();
        }
    }

}
