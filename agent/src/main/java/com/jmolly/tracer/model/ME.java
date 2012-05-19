package com.jmolly.tracer.model;

import java.util.ArrayList;
import java.util.List;

public final class ME {

    public static ME c(TH th, IN in, CL cl, long time, List<Object> args) {
        return new ME(th, in, cl, time, args);
    }

    public final TH th;
    public final IN in;
    public final CL cl;
    public final long time;
    public final List<String> args;

    public ME(TH th, IN in, CL cl, long time, List<Object> args) {
        this.th = th;
        this.in = in;
        this.cl = cl;
        this.time = time;
        this.args = new ArrayList<String>(args.size());
        for (Object arg : args) {
            this.args.add(arg.toString());
        }
    }

    @Override
    public String toString() {
        return "ME(" + th + "," + in + "," + cl + "," + time + "," + args + ")";
    }

}
