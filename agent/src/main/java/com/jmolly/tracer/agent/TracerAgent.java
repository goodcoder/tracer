package com.jmolly.tracer.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

public final class TracerAgent {

    private static final int DEFAULT_PORT = 5454;

    private static volatile TracerClassTransformer transformer;
    private static volatile TracerServer server;

    public static void premain(String args, Instrumentation instrumentation) {
        Map<String, String> mapArgs = parseArgs(args);
        int port = getInt("port", mapArgs, DEFAULT_PORT);
        Utils.logEnabled = getBool("log", mapArgs, false);
        transformer = new TracerClassTransformer(instrumentation);
        server = new TracerServer(transformer, port);
        server.start();
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        // fyi - on reload of agent a different Instrumentation instance is provided
        if (server != null) { // agent is being reloaded
            transformer.uninstall();
            server.shutdownServer();
        }
        Map<String, String> mapArgs = parseArgs(args);
        int port = getInt("port", mapArgs, DEFAULT_PORT);
        Utils.logEnabled = getBool("log", mapArgs, false);
        transformer = new TracerClassTransformer(instrumentation);
        server = new TracerServer(transformer, port);
        server.start();
    }

    private static Map<String, String> parseArgs(String args) {
        Map<String, String> asMap = new HashMap<String, String>();
        String[] parts = args.split(",");
        for (String part : parts) {
            String[] pair = part.split("=");
            if (pair.length != 2) {
                throw new RuntimeException("Couldn't parse agent option: " + part);
            }
            asMap.put(pair[0], pair[1]);
        }
        return asMap;
    }

    private static int getInt(String key, Map<String, String> map, int backup) {
        return map.containsKey(key) ? Integer.parseInt(map.get(key)) : backup;
    }

    private static boolean getBool(String key, Map<String, String> map, boolean backup) {
        return map.containsKey(key) ? Boolean.parseBoolean(map.get(key)) : backup;
    }

}
