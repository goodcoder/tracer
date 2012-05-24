package com.jmolly.tracer.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
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
import java.util.HashSet;
import java.util.Set;

public final class TracerClassTransformer implements ClassFileTransformer {

    private final ClassPool classPool = ClassPool.getDefault();
    private final Set<ClassLoader> classLoaders = new HashSet<ClassLoader>();
    private int numClassesTransformed = 0;

    private final Instrumentation instrumentation;
    private boolean installed = false;

    public TracerClassTransformer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public synchronized void install() {
        if (!installed) {
            installed = true;
            instrumentation.addTransformer(this, true);
            retransformClassesIfNeeded();
        }
    }

    public synchronized void uninstall() {
        installed = false;
        instrumentation.removeTransformer(this);
    }

    public void retransformClassesIfNeeded() {
        if (!installed) {
            return;
        }
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

    public synchronized byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classbytes) throws IllegalClassFormatException {
        if (!installed) {
            // after removing a transformer, we may still be invoked (see removeTransformer javadoc)
            return null;
        }
        if (loader.getParent() == null) {
            // assume bootstrap classloader
            Utils.log("not instrumenting class from bootstrap classloader: " + className);
            return null;
        }
        if (className.startsWith("java/")
                || className.startsWith("com/jmolly/")
                || className.startsWith("sun/")) {
            return null;
        }
        synchronized (classLoaders) {
            if (!classLoaders.contains(loader)) {
                classLoaders.add(loader);
                classPool.insertClassPath(new LoaderClassPath(loader));
            }
        }
        try {
            ByteArrayInputStream inBytes = new ByteArrayInputStream(classbytes);
            CtClass ctClass = classPool.makeClass(inBytes);
            if (ctClass.getAttribute("com.jmolly.tracer") != null) {
                // we've already instrumented this class
                return null;
            }
            ctClass.setAttribute("com.jmolly.tracer", new byte[] { 1 });
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if ((method.getModifiers() & Modifier.ABSTRACT) == 0
                        && (method.getModifiers() & Modifier.NATIVE) == 0) {
                    final boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
                    final String javaClassName = Utils.toJavaName(className);
                    final String methodName = method.getName();
                    method.insertBefore(
                        "{ com.jmolly.tracer.agent.Sink.me(" + (isStatic ? "null" : "this") +
                            ",\"" + javaClassName + "\", \"" + methodName + "\", $args); }");
                    // outermost catch to observe throwables; rethrows
                    method.addCatch("{ com.jmolly.tracer.agent.Sink.eo($e); throw $e; }",
                        classPool.get("java.lang.Throwable"));
                    method.insertAfter(
                        "{ com.jmolly.tracer.agent.Sink.mx(" + (isStatic ? "null" : "this") +
                            ",\"" + javaClassName + "\", \"" + methodName + "\", $_); }", true/*asFinally*/);
                    method.instrument(new ExprEditor() {
                        @Override
                        public void edit(Handler h) throws CannotCompileException {
                            try {
                                if (h.getType() == null) { // this is a finally block Handler
                                    return;
                                }
                            } catch (NotFoundException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                            h.insertBefore(
                                "{ com.jmolly.tracer.agent.Sink.ct(" + (isStatic ? "null" : "this") +
                                    ",\"" + javaClassName + "\", \"" + methodName + "\", $1); }");
                        }
                    });
                }
            }
            byte[] outBytes = ctClass.toBytecode();
            ctClass.detach();
            ++numClassesTransformed;
            if (numClassesTransformed % 1000 == 0) {
                Utils.log(numClassesTransformed + " total classes transformed");
            }
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
