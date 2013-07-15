package com.koushikdutta.ion.loader;

import android.net.Uri;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.stream.InputStreamDataEmitter;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

import java.io.InputStream;

/**
 * Created by koush on 5/22/13.
 */
public class ContentLoader implements Loader {
    private static final class InputStreamDataEmitterFuture extends SimpleFuture<DataEmitter> {
    }

    @Override
    public Future<DataEmitter> load(final Ion ion, final AsyncHttpRequest request, final FutureCallback<LoaderEmitter> callback) {
        if (!request.getUri().getScheme().startsWith("content"))
            return null;

        final InputStreamDataEmitterFuture ret = new InputStreamDataEmitterFuture();
        ion.getHttpClient().getServer().post(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream stream = ion.getContext().getContentResolver().openInputStream(Uri.parse(request.getUri().toString()));
                    if (stream == null)
                        throw new Exception("Unable to load content stream");
                    int available = stream.available();
                    InputStreamDataEmitter emitter = new InputStreamDataEmitter(ion.getHttpClient().getServer(), stream);
                    ret.setComplete(emitter);
                    callback.onCompleted(null, new LoaderEmitter(emitter, available, LoaderEmitter.LOADED_FROM_CACHE, null));
                }
                catch (Exception e) {
                    ret.setComplete(e);
                    callback.onCompleted(e, null);
                }
            }
        });
        return ret;
    }
}
