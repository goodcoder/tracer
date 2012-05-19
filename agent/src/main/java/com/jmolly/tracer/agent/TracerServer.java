package com.jmolly.tracer.agent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class TracerServer {

    private final Executor executor = Executors.newFixedThreadPool(5);

    public void go() {
        try {
            ServerSocket server = new ServerSocket(5555); // todo -- port is configuration
            while (true) {
                Socket socket = server.accept();
                executor.execute(new Handler(socket));
            }
        } catch (IOException e) {
            // todo - handling
            throw new RuntimeException(e);
        }
    }

    /** Handler. */
    private static final class Handler implements Runnable {

        public Handler(Socket socket) {

        }

        public void run() {

        }

    }

}
