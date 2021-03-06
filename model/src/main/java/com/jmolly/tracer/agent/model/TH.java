package com.jmolly.tracer.agent.model;

public final class TH { // thread info

    public static TH c(String id, String name, int relativeStackDepth) {
        return new TH(id, name, relativeStackDepth);
    }

    public final String id;
    public final String name;
    public final int relativeStackDepth;

    private TH(String id, String name, int relativeStackDepth) {
        this.id = id;
        this.name = name;
        this.relativeStackDepth = relativeStackDepth;
    }

    @Override
    public String toString() {
        return "TH(" + id + "," + name + ")";
    }

}
