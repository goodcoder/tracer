package com.jmolly.tracer.agent.model;

public final class TH { // thread info

    public static TH c(String id, String name) {
        return new TH(id, name);
    }

    public final String id;
    public final String name;

    private TH(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "TH(" + id + "," + name + ")";
    }

}
