package com.jmolly.tracer.model;

public final class IN {

    public static IN c(String id, String tname) {
        return new IN(id, tname);
    }

    public final String id;
    public final String tname;

    private IN(String id, String tname) {
        this.id = id;
        this.tname = tname;
    }

    @Override
    public String toString() {
        return "IN(" + id + "," + tname + ")";
    }

}
