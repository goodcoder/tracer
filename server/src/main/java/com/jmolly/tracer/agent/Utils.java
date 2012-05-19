package com.jmolly.tracer.agent;

public class Utils {

    private Utils() {}

    public static String toJavaName(String className) {
        return className.replaceAll("/", ".");
    }

}
