package com.jmolly.tracer.agent;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.WeakHashMap;

public final class TracerServer extends Thread {

    private final TracerClassTransformer transformer;
    // use this handlers set as a lock guarding both this set and the traceStreamHandler
    private final WeakHashMap<UpstreamHandler, UpstreamHandler> handlers =
        new WeakHashMap<UpstreamHandler, UpstreamHandler>();
    private final ServerSocket serverSocket;
    private TraceStreamHandler traceStreamHandler = null;

    TracerServer(TracerClassTransformer transformer, int port) {
        // server is daemon but handlers are not - by not using a thread pool target jvm can terminate
        // when there are no more client handler threads
        setDaemon(true);
        this.transformer = transformer;
        try {
            Utils.log("Starting server on port " + port);
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                synchronized (handlers) {
                    UpstreamHandler handler = new UpstreamHandler(socket);
                    handlers.put(handler, handler);
                    handler.start();
                }
            }
        } catch (SocketException e) {
            // assume due to stopServer / socket closed
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void shutdownServer() {
        try {
            serverSocket.close(); // kill the server
            synchronized (handlers) {
                for (UpstreamHandler handler : handlers.keySet()) {
                    handler.stopHandler(); // kill each handler
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // fall-thru; best effort to close socket
        }
    }

    private void stopTraceStreamHandler() {
        synchronized (handlers) {
            if (traceStreamHandler != null) {
                traceStreamHandler.stopHandler();
                traceStreamHandler = null;
            }
        }
    }

    /** Upstream handler. */
    private final class UpstreamHandler extends Thread {

        private final Socket socket;
        private final BufferedReader upstream;
        private final PrintWriter downstream;

        public UpstreamHandler(Socket socket) throws IOException {
            setDaemon(false);
            this.socket = socket;
            this.upstream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.downstream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        }

        public void run() {
            try {
                String line;
                while ((line = upstream.readLine()) != null) {
                    if (line.startsWith("PING")) {
                        synchronized (handlers) {
                            if (traceStreamHandler == null || !traceStreamHandler.isOwner(this)) {
                                // only support ping (sending output) if we're not streaming a trace to this client
                                downstream.println("PONG");
                            }
                        }
                    } else if (line.startsWith("CONFIGURE")) {
                        long start = System.currentTimeMillis();
                        transformer.retransformClassesIfNeeded();
                        long time = System.currentTimeMillis() - start;
                        synchronized (handlers) {
                            if (traceStreamHandler == null || !traceStreamHandler.isOwner(this)) {
                                // only support sending output if we're not streaming a trace to this client
                                downstream.println("OK " + time + "ms");
                            }
                        }
                    } else if (line.startsWith("START")) { // start trace
                        synchronized (handlers) {
                            if (traceStreamHandler == null) { // START is ignored if we're already streaming
                                traceStreamHandler = new TraceStreamHandler(this, downstream);
                                traceStreamHandler.start();
                            } else if (!traceStreamHandler.isOwner(this)) {
                                downstream.println("ERROR another client is streaming");
                            }
                        }
                    } else if (line.startsWith("STOP")) { // stop trace
                        stopTraceStreamHandler();
                    } else if (line.startsWith("DISCONNECT")) {
                        synchronized (handlers) {
                            if (traceStreamHandler != null && traceStreamHandler.isOwner(this)) {
                                // this client is the owner
                                stopTraceStreamHandler();
                            }
                        }
                        this.stopHandler();
                    }
                }
            } catch (IOException e) {
                // okay
            } finally {
                Utils.log("Exiting UpstreamHandler");
            }
        }

        public void stopHandler() {
            try {
                socket.close();
            } catch (IOException e) {
                // continue; we did a best-effort
            }
        }

    }

    /** Downstream handler. */
    private final class TraceStreamHandler extends Thread {

        private final Gson gson = new Gson();

        private final Object owner;
        private final JsonWriter jsonWriter;

        public TraceStreamHandler(Object owner, PrintWriter downstream) throws IOException {
            setDaemon(false);
            this.owner = owner;
            this.jsonWriter = new JsonWriter(downstream);
        }

        public boolean isOwner(Object candidate) {
            return candidate == owner;
        }

        public void run() {
            try {
                Utils.assertFalse(Sink.isActive());
                Sink.activate();
                while (!this.isInterrupted()) {
                    Object took = Sink.take();
                    gson.toJson(took, took.getClass(), jsonWriter);
                    jsonWriter.flush();
                }
            } catch (InterruptedException e) {
                // okay
            } catch (JsonIOException e) {
                Utils.log(e);
            } catch (IOException e) {
                Utils.log(e);
            } finally {
                Utils.log("Exiting TraceStreamHandler");
            }
        }

        public void stopHandler() {
            Sink.deactivate();
            this.interrupt();
            try {
                jsonWriter.flush();
            } catch (IOException e) {
                Utils.log(e);
            }
        }

    }

}
