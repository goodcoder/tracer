package com.jmolly.tracer.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

public final class TracerAgent {

    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("PREMAIN");
        final TracerClassTransformer transformer = new TracerClassTransformer(instrumentation);
        instrumentation.addTransformer(transformer, true);
        // todo restartServer(parseArgs(args));
    }

    public static void agentmain(String args, final Instrumentation instrumentation) throws Exception {
        System.out.println("AGENTMAIN");
        final TracerClassTransformer transformer = new TracerClassTransformer(instrumentation);
        instrumentation.addTransformer(transformer, true);
        transformer.reconfigure();
        // todo restartServer(parseArgs(args));
//        new Thread() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                instrumentation.addTransformer(transformer, true /* can retransform */);
//                transformer.reconfigure(null);
//            }
//        }.start();
    }

    private static void restartServer(Map<String, Object> args) {
        // HeatlampServer.instance.restartOn((Integer) args.get("port"));
    }

    private static Map<String, Object> parseArgs(String args) {
        Map<String, Object> asMap = new HashMap<String, Object>();
        String[] parts = args.split(",");
        for (String part : parts) {
            String[] pair = part.split("=");
            if (pair.length != 2) {
                throw new RuntimeException("Couldn't parse agent option: " + part);
            }
            asMap.put(pair[0], pair[1]);
        }
        return validateArgs(asMap);
    }

    private static Map<String, Object> validateArgs(Map<String, Object> asMap) {
        String port = (String) asMap.get("port");
        if (port == null) {
            throwUp("Must provide agent option 'port'");
        }
        try {
            asMap.put("port", Integer.parseInt(port));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Bad agent option 'port'", e);
        }
        return asMap;
    }

    private static void throwUp(String msg) {
        throw new RuntimeException(msg);
    }

}
