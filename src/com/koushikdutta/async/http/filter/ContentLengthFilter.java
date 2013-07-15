package com.koushikdutta.async.http.filter;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FilteredDataEmitter;

public class ContentLengthFilter extends FilteredDataEmitter {
    public ContentLengthFilter(int contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    protected void report(Exception e) {
        if (e == null && totalRead != contentLength)
            e = new Exception("End of data reached before content length was read");
        super.report(e);
    }

    int contentLength;
    int totalRead;
    ByteBufferList transformed = new ByteBufferList();
    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        assert totalRead < contentLength;
        int remaining = bb.remaining();
        int toRead = Math.min(contentLength - totalRead, remaining);

        bb.get(transformed, toRead);

        totalRead += transformed.remaining();
        super.onDataAvailable(emitter, transformed);
        if (totalRead == contentLength)
            report(null);
    }
}
