package com.jmolly.tracer.agent;

import com.jmolly.tracer.agent.model.CL;
import com.jmolly.tracer.agent.model.CT;
import com.jmolly.tracer.agent.model.EO;
import com.jmolly.tracer.agent.model.IN;
import com.jmolly.tracer.agent.model.ME;
import com.jmolly.tracer.agent.model.MX;
import com.jmolly.tracer.agent.model.TH;

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

    public static Object poll() {
        return queue.poll();
    }

    /** Method entry. Provided instance is null if static invocation. */
    public static void me(Object instance, String methodDeclaringClass, String methodName, Object[] args) {
        try {
            if (active && !within.get()) { // ensure that no invocations we make within this method trigger infinite recursion
                within.set(true);
                boolean isStatic = null == instance;
                Thread ct = Thread.currentThread();
                queue.add(
                    ME.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName()),
                        isStatic ? IN.c(methodDeclaringClass, methodDeclaringClass)
                                : IN.c(String.valueOf(System.identityHashCode(instance)), instance.getClass().getName()),
                        CL.c(methodDeclaringClass, methodName),
                        System.currentTimeMillis(),
                        args
                    )
                );
            }
        } finally {
            within.set(false);
        }
    }

    public static void eo(Object e) {
        try {
            if (active && !within.get()) { // ensure that no invocations we make within this method trigger infinite recursion
                within.set(true);
                Thread ct = Thread.currentThread();
                queue.add(
                    EO.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName()),
                        e.getClass().getName()
                    )
                );
            }
        } finally {
            within.set(false);
        }
    }

    public static void mx(Object instance, String methodDeclaringClass, String methodName, Object rv) {
        try {
            if (active && !within.get()) { // ensure that no invocations we make within this method trigger infinite recursion
                within.set(true);
                boolean isStatic = null == instance;
                Thread ct = Thread.currentThread();
                queue.add(
                    MX.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName()),
                        isStatic ? IN.c(methodDeclaringClass, methodDeclaringClass)
                                 : IN.c(String.valueOf(System.identityHashCode(instance)), instance.getClass().getName()),
                        CL.c(methodDeclaringClass, methodName),
                        String.valueOf(rv),
                        System.currentTimeMillis()
                    )
                );
            }
        } finally {
            within.set(false);
        }
    }

    public static void ct(Object instance, String methodDeclaringClass, String methodName, Object e) {
        try {
            if (active && !within.get()) { // ensure that no invocations we make within this method trigger infinite recursion
                within.set(true);
                boolean isStatic = null == instance;
                Thread ct = Thread.currentThread();
                queue.add(
                    CT.c(
                        TH.c(String.valueOf(ct.getId()), ct.getName()),
                        isStatic ? IN.c(methodDeclaringClass, methodDeclaringClass)
                                : IN.c(String.valueOf(System.identityHashCode(instance)), instance.getClass().getName()),
                        CL.c(methodDeclaringClass, methodName),
                        e.getClass().getName(),
                        System.currentTimeMillis()
                    )
                );
            }
        } finally {
            within.set(false);
        }
    }

}
