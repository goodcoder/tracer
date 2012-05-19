package com.jmolly.tracer.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public final class TracerClassTransformer implements ClassFileTransformer {

    private static final String PKG_AGENT = "com.jmolly.tracer.agent";
    private static final String PKG_MODEL = "com.jmolly.tracer.agent.model";

    // ClassPool with system classpath, as we want to inject bytecode that calls system classes
    private final ClassPool pool = new ClassPool(true);
    private final Instrumentation instrumentation;

    public TracerClassTransformer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public void reconfigure() {
        try {
            Class[] cs = instrumentation.getAllLoadedClasses();
            for (Class c : cs) {
                if (instrumentation.isModifiableClass(c)) {
                    instrumentation.retransformClasses(c);
                }
            }
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classbytes) throws IllegalClassFormatException {
        if (className.startsWith("java/")
                || className.startsWith("com/jmolly/")
                || className.startsWith("sun/")) {
            return null;
        }
        try {
            ByteArrayInputStream inBytes = new ByteArrayInputStream(classbytes);
            CtClass ctClass = pool.makeClass(inBytes);
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if ((method.getModifiers() & Modifier.ABSTRACT) == 0
                        && (method.getModifiers() & Modifier.NATIVE) == 0) {
                    final boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
                    final String javaClassName = Utils.toJavaName(className);
                    final String methodName = method.getName();
                    method.insertBefore(
                        "{ Thread ct = Thread.currentThread(); " +
                            PKG_AGENT + ".Sink.put(" +
                            PKG_MODEL + ".ME.c(" +
                                PKG_MODEL + ".TH.c(" +
                                    "String.valueOf(ct.getId()), ct.getName()" +
                                ")," + // TH.c
                                PKG_MODEL + ".IN.c(" +
                                    // instance id is type classname if static invocation
                                    (isStatic ?
                                        ("\"" + javaClassName + "\", \"" + javaClassName + "\"") :
                                        ("\"String.valueOf(System.identityHashCode())\", $0.getClass().getName()")) +
                                ")," + // IN.c
                                PKG_MODEL + ".CL.c(" +
                                    "\"" + javaClassName + "\",\"" + methodName + "\"" +
                                ")," + // CL.c
                                "System.currentTimeMillis()," +
                                "java.util.Arrays.asList($args)" +
                            ")" + // ME.c
                        "); }" // Sink.put
                    );
                    // outermost catch to observe throwables; rethrows
                    method.addCatch("{" +
                        PKG_AGENT + ".Sink.put(" +
                                PKG_MODEL + ".EO.c($e.getClass().getName())" +
                            ");" + // Sink.put
                            "throw $e;" +
                    "}", pool.get("java.lang.Throwable"));
                    method.insertAfter(
                        "{ Thread ct = Thread.currentThread(); " +
                            PKG_AGENT + ".Sink.put(" +
                                PKG_MODEL + ".MX.c(" +
                                    PKG_MODEL + ".TH.c(" +
                                        "String.valueOf(ct.getId()), ct.getName()" +
                                    ")," + // TH.c
                                    PKG_MODEL + ".IN.c(" +
                                        // instance id is type classname if static invocation
                                        (isStatic ?
                                            ("\"" + javaClassName + "\", \"" + javaClassName + "\"") :
                                            ("\"String.valueOf(System.identityHashCode())\", $0.getClass().getName()")) +
                                    ")," + // IN.c
                                    PKG_MODEL + ".CL.c(" +
                                        "\"" + javaClassName + "\",\"" + methodName + "\"" +
                                    ")," + // CL.c
                                    "String.valueOf($_)," + // $_ is null if void or null/0 if throwing
                                    "System.currentTimeMillis()" +
                                ")" + // MX.c
                        ");}", // Sink.put
                    true);
                    method.instrument(new ExprEditor() {
                        @Override
                        public void edit(Handler h) throws CannotCompileException {
                            try {
                                if (h.getType() == null) { // this is a finally block Handler
                                    return;
                                }
                            } catch (NotFoundException e) {
                                // todo -- how to guarantee this never happens? do we need to create ClassPool
                                // todo -- hierarchy tree for classloader hierarchy trees?
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                            h.insertBefore(
                                "{ Thread ct = Thread.currentThread(); " +
                                    PKG_AGENT + ".Sink.put(" +
                                        PKG_MODEL + ".CT.c(" +
                                            PKG_MODEL + ".TH.c(" +
                                                "String.valueOf(ct.getId()), ct.getName()" +
                                            ")," + // TH.c
                                            PKG_MODEL + ".IN.c(" +
                                                // instance id is type classname if static invocation
                                                (isStatic ?
                                                    ("\"" + javaClassName + "\", \"" + javaClassName + "\"") :
                                                    ("\"String.valueOf(System.identityHashCode())\", $0.getClass().getName()")) +
                                            ")," + // IN.c
                                            PKG_MODEL + ".CL.c(" +
                                                "\"" + javaClassName + "\",\"" + methodName + "\"" +
                                            ")," + // CL.c
                                            "$1.getClass().getName()," +
                                            "System.currentTimeMillis()" +
                                        ")" + // CT.c
                                ");}" // Sink.put
                            );
                        }
                    });
                }
            }
            byte[] outBytes = ctClass.toBytecode();
            ctClass.detach();
            return outBytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CannotCompileException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
