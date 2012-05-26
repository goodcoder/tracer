package com.jmolly.tracer.demo;

import com.jmolly.tracer.client.EventStream;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Demo {

    private static final String AGENT_JARNAME = "tracer-agent.jar";

    public static void main(String[] args) throws Exception {
        File agent = new File(AGENT_JARNAME);
        if (!agent.isFile()) {
            throw new RuntimeException("Expected " + AGENT_JARNAME + " in working directoy.");
        }
        loadAgent(agent, args[0]);
        trace();
    }

    private static void trace() throws Exception {
        Socket socket = new Socket("localhost", 5555);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println("CONF");
        String line = in.readLine();
        if (!line.startsWith("OK")) {
            throw new RuntimeException("unexpected: " + line);
        }
        out.println("GO");
        EventStream events = new EventStream(socket.getInputStream());
        while (events.hasNext()) {
            System.out.println("[from agent] " + events.readEvent());
        }
        in.close();
    }

    private static void loadAgent(File file, String pid) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(file.getAbsolutePath(), "port=5555,log=true");
            vm.detach();
        } catch (AttachNotSupportedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (AgentInitializationException e) {
            throw new RuntimeException(e);
        } catch (AgentLoadException e) {
            throw new RuntimeException(e);
        }
    }

}
