package com.jmolly.tracer.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public final class EventStream extends InputStream {

    private final Gson gson = new GsonBuilder().create();
    private final InputStream bin;
    private final JsonStreamParser parser;

    public EventStream(InputStream bin) {
        this(bin, "UTF-8");
    }

    private EventStream(InputStream bin, String charsetName) {
        this.bin = bin;
        this.parser = new JsonStreamParser(toReader(bin, charsetName));
    }

    public boolean hasNext() {
        return parser.hasNext();
    }

    public <T> T readType(Class<T> type) {
        final JsonElement element = parser.next();
        return gson.fromJson(element, type);
    }

    public Object readEvent() {
        final JsonElement element = parser.next();
        return gson.fromJson(element,
            TracerTypes.toType(getTypeProperty(element)));
    }

    @Override
    public int read() throws IOException {
        return bin.read();
    }

    @Override
    public void close() throws IOException {
        bin.close();
    }

    private static String getTypeProperty(JsonElement element) {
        return element.getAsJsonObject().get("type").getAsString();
    }

    private static InputStreamReader toReader(InputStream bin, String charsetName) {
        try {
            return new InputStreamReader(bin, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

}
