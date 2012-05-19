package com.jmolly.tracer.client;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;

public class Client {

    private static final String AGENT_JARNAME = "tracer-agent.jar";

    public static void main(String[] args) {
        File agent = new File(new File("."), AGENT_JARNAME);
        if (!agent.isFile()) {
            throw new RuntimeException("Expected " + AGENT_JARNAME + " in working directoy.");
        }
        loadAgent(agent, args[0]);
    }

    private static void loadAgent(File file, String pid) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(file.getAbsolutePath());
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
