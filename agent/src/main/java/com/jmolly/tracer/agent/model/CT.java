package com.jmolly.tracer.agent.model;

public final class CT { // catch

    public static CT c(TH th, IN in, CL cl, String tname, long time) {
        return new CT(th, in, cl, tname, time);
    }

    public final TH th;
    public final IN in;
    public final CL cl;
    public final String tname; // caught typename
    public final long time;

    private CT(TH th, IN in, CL cl, String tname, long time) {
        this.th = th;
        this.in = in;
        this.cl = cl;
        this.tname = tname;
        this.time = time;
    }

    @Override
    public String toString() {
        return "CT(" + th + "," + in + "," + cl + "," + tname + "," + time + ")";
    }

}
