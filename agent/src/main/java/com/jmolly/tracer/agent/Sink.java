package com.jmolly.tracer.agent;

import com.jmolly.tracer.agent.model.CL;
import com.jmolly.tracer.agent.model.CT;
import com.jmolly.tracer.agent.model.EO;
import com.jmolly.tracer.agent.model.IN;
import com.jmolly.tracer.agent.model.ME;
import com.jmolly.tracer.agent.model.MX;
import com.jmolly.tracer.agent.model.TH;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Sink {

    /** Must be non-blocking queue! */
    private static final Queue<Object> queue = new ConcurrentLinkedQueue<Object>();

    private static final ThreadLocal<Boolean> within = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static volatile ThreadLocal<int[]> relativeStackDepth;
    private static volatile boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void activate() {
        relativeStackDepth = new ThreadLocal<int[]>() {
            @Override
            protected int[] initialValue() {
                return new int[] { 1000 };
            }
        };
        queue.clear();
        active = true;
    }

    public static void deactivate() {
        active = false;
        queue.clear();
    }

    public static Object poll() {
        return queue.poll();
    }

    public static void me(Object instance, String methodDeclaringClass, String methodName, Object[] args) {
        if (active && !within.get()) {
            try {
                within.set(true);
                int depth = ++relativeStackDepth.get()[0];
                boolean isStatic = null == instance;
                Thread ct = Thread.currentThread();
                queue.add(
                    ME.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName(), depth),
                        isStatic ? IN.c(methodDeclaringClass, methodDeclaringClass)
                                 : IN.c(String.valueOf(System.identityHashCode(instance)), instance.getClass().getName()),
                        CL.c(methodDeclaringClass, methodName),
                        System.currentTimeMillis(),
                        toArgs(args)
                    )
                );
            } finally {
                within.set(false);
            }
        }
    }

    public static void eo(Object e) {
        if (active && !within.get()) {
            try {
                within.set(true);
                int depth = relativeStackDepth.get()[0]--;
                Thread ct = Thread.currentThread();
                queue.add(
                    EO.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName(), depth),
                        e.getClass().getName()
                    )
                );
            } finally {
                within.set(false);
            }
        }
    }

    public static void mx(Object instance, String methodDeclaringClass, String methodName, Object rv) {
        if (active && !within.get()) { // ensure that no invocations we make within this method trigger infinite recursion
            try {
                within.set(true);
                int depth = relativeStackDepth.get()[0]--;
                long time = System.currentTimeMillis();
                boolean isStatic = null == instance;
                Thread ct = Thread.currentThread();
                queue.add(
                    MX.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName(), depth),
                        isStatic ? IN.c(methodDeclaringClass, methodDeclaringClass)
                                 : IN.c(String.valueOf(System.identityHashCode(instance)), instance.getClass().getName()),
                        CL.c(methodDeclaringClass, methodName),
                        toString(rv),
                        time
                    )
                );
            } finally {
                within.set(false);
            }
        }
    }

    public static void ct(Object instance, String methodDeclaringClass, String methodName, Object e) {
        if (active && !within.get()) {
            try {
                within.set(true);
                int depth = relativeStackDepth.get()[0];
                boolean isStatic = null == instance;
                Thread ct = Thread.currentThread();
                queue.add(
                    CT.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName(), depth),
                        isStatic ? IN.c(methodDeclaringClass, methodDeclaringClass)
                                : IN.c(String.valueOf(System.identityHashCode(instance)), instance.getClass().getName()),
                        CL.c(methodDeclaringClass, methodName),
                        e.getClass().getName(),
                        System.currentTimeMillis()
                    )
                );
            } finally {
                within.set(false);
            }
        }
    }


    private static List<String> toArgs(Object[] args) {
        List<String> toArgs = new ArrayList<String>(args.length);
        for (Object arg : args) {
            toArgs.add(toString(arg));
        }
        return toArgs;
    }

    private static String toString(Object o) {
        try {
            return String.valueOf(o);
        } catch (Exception e) {
            // oops, toString of object threw exception
            return "<err>";
        }
    }

}
