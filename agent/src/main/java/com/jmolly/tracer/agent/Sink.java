package com.jmolly.tracer.agent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class Sink {

    private static final BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(10000);
    private static final ThreadLocal<Boolean> sinking = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static volatile boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void activate() {
        queue.clear();
        active = true;
    }

    public static void deactivate() {
        active = false;
        queue.clear();
    }

    public static void put(Object o) {
        if (active && !sinking.get()) { // ensure that no invocations we make within this method trigger infinite recursion
            try {
                sinking.set(true);
                queue.put(o);
            } catch (InterruptedException e) {
                // okay; continue
            } finally {
                sinking.set(false);
            }
        }
    }

    public static Object take() throws InterruptedException {
        return queue.take();
    }

}
