package com.jmolly.tracer.agent.model;

public final class CL {

    public static CL c(String tname, String mname) {
        return new CL(tname, mname);
    }

    public final String tname; // typename
    public final String mname; // methodname

    private CL(String tname, String mname) {
        this.tname = tname;
        this.mname = mname;
    }

    @Override
    public String toString() {
        return "CL(" + tname + "," + mname + ")";
    }

}
