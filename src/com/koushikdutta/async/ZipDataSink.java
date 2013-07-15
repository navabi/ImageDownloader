package com.koushikdutta.async;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.koushikdutta.async.callback.CompletedCallback;

public class ZipDataSink extends FilteredDataSink {
    public ZipDataSink(DataSink sink) {
        super(sink);
    }

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ZipOutputStream zop = new ZipOutputStream(bout);
    boolean first = true;

    public void putNextEntry(ZipEntry ze) throws IOException {
        zop.putNextEntry(ze);
    }

    public void closeEntry() throws IOException {
        zop.closeEntry();
    }
    
    protected void report(Exception e) {
        CompletedCallback closed = getClosedCallback();
        if (closed != null)
            closed.onCompleted(e);
    }

    public void close() {
        try {
            zop.close();
        }
        catch (IOException e) {
            report(e);
            return;
        }
        setMaxBuffer(Integer.MAX_VALUE);
        write(new ByteBufferList());
        super.close();
    }

    @Override
    public ByteBufferList filter(ByteBufferList bb) {
        try {
            if (bb != null) {
                while (bb.size() > 0) {
                    ByteBuffer b = bb.remove();
                    zop.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
                    ByteBufferList.reclaim(b);
                }
            }
            ByteBufferList ret = new ByteBufferList(bout.toByteArray());
            bout.reset();
            return ret;
        }
        catch (IOException e) {
            report(e);
            return null;
        }
        finally {
            if (bb != null)
                bb.recycle();
        }
    }
}
