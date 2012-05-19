package com.jmolly.tracer.model;

public final class MX {

    public static MX c(TH th, IN in, CL cl, String rv, long time) {
        return new MX(th, in, cl, rv, time);
    }

    public final TH th;
    public final IN in;
    public final CL cl;
    public final String rv;
    public final long time;

    public MX(TH th, IN in, CL cl, String rv, long time) {
        this.th = th;
        this.in = in;
        this.cl = cl;
        this.rv = rv;
        this.time = time;
    }

    @Override
    public String toString() {
        return "MX(" + th + "," + in + "," + cl + "," + rv + "," + time + ")";
    }

}
