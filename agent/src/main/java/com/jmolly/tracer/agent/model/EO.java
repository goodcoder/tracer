package com.jmolly.tracer.agent.model;

public final class EO { // exception out of a method

    public static EO c(String tname) {
        return new EO(tname);
    }

    public final String tname; // exception type name

    private EO(String tname) {
        this.tname = tname;
    }

    @Override
    public String toString() {
        return "EO()";
    }

}
