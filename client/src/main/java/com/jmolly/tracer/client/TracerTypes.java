package com.jmolly.tracer.client;

import com.jmolly.tracer.agent.model.CT;
import com.jmolly.tracer.agent.model.EO;
import com.jmolly.tracer.agent.model.ME;
import com.jmolly.tracer.agent.model.MX;

final class TracerTypes {

    private TracerTypes() {}

    public static Class<?> toType(String type) {
        if (ME.type.equals(type)) {
            return ME.class;
        }
        if (MX.type.equals(type)) {
            return MX.class;
        }
        if (CT.type.equals(type)) {
            return CT.class;
        }
        if (EO.type.equals(type)) {
            return EO.class;
        }
        throw new IllegalStateException("no such type : " + type);
    }

}
