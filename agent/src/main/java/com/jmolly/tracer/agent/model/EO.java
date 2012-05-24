package com.jmolly.tracer.agent.model;

public final class EO { // exception out of a method

    public static EO c(TH th, String ename) {
        return new EO(th, ename);
    }

    public final TH th;
    public final String ename; // exception type name

    private EO(TH th, String ename) {
        this.th = th;
        this.ename = ename;
    }

    @Override
    public String toString() {
        return "EO(" + th + "," + ename + ")";
    }

}
