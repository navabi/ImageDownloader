package com.koushikdutta.async.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringPart extends StreamPart {
    String value;
    public StringPart(String name, String value) {
        super(name, value.getBytes().length, null);
        this.value = value;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(value.getBytes());
    }

}
