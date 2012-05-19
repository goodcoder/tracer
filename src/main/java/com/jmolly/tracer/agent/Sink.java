package com.jmolly.tracer.agent;

public final class Sink {

    private static final ThreadLocal<Boolean> sinking;

    static {
        sinking = new ThreadLocal<Boolean>();
        sinking.set(false);
    }

    public static void put(Object o) {
        if (!sinking.get()) { // ensure that no invocations we make within this method trigger infinite recursion
            sinking.set(true);
            try {
                System.out.println(o);
            } finally {
                sinking.set(false);
            }
        }
    }

}
