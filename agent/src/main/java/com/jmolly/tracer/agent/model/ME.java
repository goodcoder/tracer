package com.jmolly.tracer.agent.model;

import java.util.ArrayList;
import java.util.List;

public final class ME {

    public static ME c(TH th, IN in, CL cl, long time, Object[] args) {
        return new ME(th, in, cl, time, args);
    }

    public final TH th;
    public final IN in;
    public final CL cl;
    public final long time;
    public final List<String> args;

    public ME(TH th, IN in, CL cl, long time, Object[] args) {
        this.th = th;
        this.in = in;
        this.cl = cl;
        this.time = time;
        this.args = new ArrayList<String>(args.length);
        for (Object arg : args) {
            this.args.add(String.valueOf(arg));
        }
    }

    @Override
    public String toString() {
        return "ME(" + th + "," + in + "," + cl + "," + time + "," + args + ")";
    }

}
