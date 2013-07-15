package com.koushikdutta.ion;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.DataTrackingEmitter;
import com.koushikdutta.async.DataTrackingEmitter.DataTracker;
import com.koushikdutta.async.FilteredDataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.MultipartFormDataBody;
import com.koushikdutta.async.http.UrlEncodedFormBody;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.ion.Loader.LoaderEmitter;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.Builders.Any.B;
import com.koushikdutta.ion.builder.Builders.Any.BF;
import com.koushikdutta.ion.builder.FutureBuilder;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.future.ResponseFuture;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements Builders.Any.B, Builders.Any.F, Builders.Any.M, Builders.Any.U, LoadBuilder<Builders.Any.B> {
    Ion ion;
    WeakReference<Context> context;
    Handler handler = Ion.mainHandler;
    String method = AsyncHttpGet.METHOD;
    String uri;

    public IonRequestBuilder(Context context, Ion ion) {
        this.ion = ion;
        this.context = new WeakReference<Context>(context);
    }

    @Override
    public IonRequestBuilder load(String url) {
        return loadInternal(AsyncHttpGet.METHOD, url);
    }

    private IonRequestBuilder loadInternal(String method, String url) {
        this.method = method;
        this.uri = url;
        return this;
    }

    boolean methodWasSet;
    @Override
    public IonRequestBuilder load(String method, String url) {
        methodWasSet = true;
        return loadInternal(method, url);
    }

    RawHeaders headers;
    private RawHeaders getHeaders() {
        if (headers == null)
            headers = new RawHeaders();
        return headers;
    }

    @Override
    public IonRequestBuilder userAgent(String userAgent) {
        return setHeader("User-Agent", userAgent);
    }

    @Override
    public IonRequestBuilder setHeader(String name, String value) {
        getHeaders().set(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addHeader(String name, String value) {
        getHeaders().add(name, value);
        return this;
    }

    Multimap query;
    @Override
    public IonRequestBuilder addQuery(String name, String value) {
        if (query == null)
            query = new Multimap();
        query.add(name, value);
        return this;
    }

    int timeoutMilliseconds;
    @Override
    public IonRequestBuilder setTimeout(int timeoutMilliseconds) {
        this.timeoutMilliseconds = AsyncHttpRequest.DEFAULT_TIMEOUT;
        return this;
    }

    @Override
    public IonRequestBuilder setHandler(Handler handler) {
        this.handler = handler;
        return this;
    }

    AsyncHttpRequestBody body;
    private <T> IonRequestBuilder setBody(AsyncHttpRequestBody<T> body) {
        if (!methodWasSet)
            method = AsyncHttpPost.METHOD;
        this.body = body;
        return this;
    }


    boolean followRedirect = true;
    @Override
    public IonRequestBuilder followRedirect(boolean follow) {
        followRedirect = follow;
        return this;
    }

    private static boolean isServiceRunning(Service candidate) {
        ActivityManager manager = (ActivityManager)candidate.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        if (services == null)
            return false;
        for (ActivityManager.RunningServiceInfo service: services) {
            if (candidate.getClass().getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkContext() {
        Context context = IonRequestBuilder.this.context.get();
        if (context == null)
            return false;
        if (context instanceof Activity) {
            Activity activity = (Activity)context;
            if (activity.isFinishing())
                return false;
        }
        else if (context instanceof Service) {
            Service service = (Service)context;
            if (!isServiceRunning(service))
                return false;
        }

        return true;
    }

    private <T> void postExecute(final SimpleFuture<T> future, final Exception ex, final T value) {
        final Runnable runner = new Runnable() {
            @Override
            public void run() {
                // check if the context is still alive...
                if (!checkContext())
                    return;

                // unless we're invoked onto the handler/main/service thread, there's no frakking way to avoid a
                // race condition where the service or activity dies before this callback is invoked.
                if (ex != null)
                    future.setComplete(ex);
                else
                    future.setComplete(value);
            }
        };

        if (handler == null)
            ion.httpClient.getServer().post(runner);
        else
            AsyncServer.post(handler, runner);
    }

    private <T> void getLoaderEmitter(final EmitterTransform<T> ret) {
        URI uri;
        try {
            if (query != null) {
                Uri.Builder builder = Uri.parse(this.uri).buildUpon();
                for (String key: query.keySet()) {
                    for (String value: query.get(key)) {
                        builder = builder.appendQueryParameter(key, value);
                    }
                }
                uri = URI.create(builder.toString());
            }
            else {
                uri = URI.create(this.uri);
            }
        }
        catch (Exception e) {
            uri = null;
        }
        if (uri == null || uri.getScheme() == null) {
            ret.setComplete(new Exception("Invalid URI"));
            return;
        }


        AsyncHttpRequest request = new AsyncHttpRequest(uri, method, headers);
        request.setFollowRedirect(followRedirect);
        request.setLogging(ion.LOGTAG, ion.logLevel);
        if (logTag != null)
            request.setLogging(logTag, logLevel);
        request.enableProxy(proxyHost, proxyPort);
        request.setTimeout(timeoutMilliseconds);
        request.setHandler(null);
        request.logd("preparing request");

        ret.request = request;

        for (Loader loader: ion.config.loaders) {
            Future<DataEmitter> emitter = loader.load(ion, request, ret);
            if (emitter != null) {
                ret.setParent(emitter);
                return;
            }
        }
        ret.setComplete(new Exception("Unknown uri scheme"));
    }

    class EmitterTransform<T> extends TransformFuture<T, LoaderEmitter> implements ResponseFuture<T> {
        private AsyncHttpRequest request;
        private int loadedFrom;
        Runnable cancelCallback;
        RawHeaders headers;
        DataEmitter emitter;

        class ResponseFutureImpl extends TransformFuture<Response<T>, T> {
            @Override
            protected void error(Exception e) {
                Response<T> response = new Response<T>();
                response.headers = headers;
                setComplete(e, response);
            }

            @Override
            protected void transform(T result) throws Exception {
                Response<T> response = new Response<T>();
                response.headers = headers;
                response.result = result;
                setComplete(response);
            }
        }

        @Override
        public Future<Response<T>> withResponse() {
            final ResponseFutureImpl ret = new ResponseFutureImpl();
            setCallback(ret);
            return ret;
        }

        public int loadedFrom() {
            return loadedFrom;
        }

        public EmitterTransform(Runnable cancelCallback) {
            this.cancelCallback = cancelCallback;
            ion.addFutureInFlight(this, context.get());
            if (groups == null)
                return;
            for (WeakReference<Object> ref: groups) {
                Object group = ref.get();
                if (group != null)
                    ion.addFutureInFlight(this, group);
            }
        }

        @Override
        protected void cancelCleanup() {
            super.cancelCleanup();
            if (emitter != null)
                emitter.close();
            if (cancelCallback != null)
                cancelCallback.run();
        }

        @Override
        protected void error(Exception e) {
            // don't call superclass which calls setComplete... get onto handler thread.
            postExecute(this, e, null);
        }

        @Override
        protected void transform(LoaderEmitter emitter) throws Exception {
            this.emitter = emitter.getDataEmitter();
            this.loadedFrom = emitter.loadedFrom();
            this.headers = emitter.getHeaders();

            if (headersCallback != null) {
                final RawHeaders headers = emitter.getHeaders();
                // what do we do on loaders that don't have headers? files, content://, etc.
                AsyncServer.post(handler, new Runnable() {
                    @Override
                    public void run() {
                        headersCallback.onHeaders(headers);
                    }
                });
            }

            // hook up data progress callbacks
            final int total = emitter.length();
            DataTrackingEmitter tracker;
            if (!(emitter instanceof DataTrackingEmitter)) {
                tracker = new FilteredDataEmitter();
                tracker.setDataEmitter(this.emitter);
            }
            else {
                tracker = (DataTrackingEmitter)this.emitter;
            }
            this.emitter = tracker;
            tracker.setDataTracker(new DataTracker() {
                @Override
                public void onData(final int totalBytesRead) {
                    assert Thread.currentThread() != Looper.getMainLooper().getThread();
                    // if the requesting context dies during the transfer... cancel
                    if (!checkContext()) {
                        cancel();
                        return;
                    }

                    int percent = (int)((float)totalBytesRead / total * 100f);

                    if (progressBar != null) {
                        ProgressBar bar = progressBar.get();
                        if (bar != null)
                            bar.setProgress(percent);
                    }
                    if (progressDialog != null) {
                        ProgressDialog dlg = progressDialog.get();
                        if (dlg != null)
                            dlg.setProgress(percent);
                    }

                    if (progress != null)
                        progress.onProgress(totalBytesRead, total);

                    if (progressHandler != null) {
                        AsyncServer.post(handler, new Runnable() {
                            @Override
                            public void run() {
                                if (isCancelled() || isDone())
                                    return;
                                progressHandler.onProgress(totalBytesRead, total);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public IonRequestBuilder progressBar(ProgressBar progressBar) {
        this.progressBar = new WeakReference<ProgressBar>(progressBar);
        return this;
    }

    @Override
    public IonRequestBuilder progressDialog(ProgressDialog progressDialog) {
        this.progressDialog = new WeakReference<ProgressDialog>(progressDialog);
        return this;
    }

    WeakReference<ProgressBar> progressBar;
    WeakReference<ProgressDialog> progressDialog;

    ProgressCallback progress;
    @Override
    public IonRequestBuilder progress(ProgressCallback callback) {
        progress = callback;
        return this;
    }

    ProgressCallback progressHandler;
    @Override
    public IonRequestBuilder progressHandler(ProgressCallback callback) {
        progressHandler = callback;
        return this;
    }

    <T> EmitterTransform<T> execute(final DataSink sink, final boolean close, final T result) {
        return execute(sink, close, result, null);
    }


    <T> EmitterTransform<T> execute(final DataSink sink, final boolean close, final T result, final Runnable cancel) {
        EmitterTransform<T> ret = new EmitterTransform<T>(cancel) {
            @Override
            protected void cancelCleanup() {
                super.cancelCleanup();
                if (close)
                    sink.close();
            }

            TransformFuture<T, LoaderEmitter> self = this;
            @Override
            protected void transform(LoaderEmitter emitter) throws Exception {
                super.transform(emitter);
                Util.pump(this.emitter, sink, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (close)
                            sink.close();
                        postExecute(self, ex, result);
                    }
                });
            }
        };
        getLoaderEmitter(ret);
        return ret;
    }

    <T> EmitterTransform<T> execute(final AsyncParser<T> parser) {
        return execute(parser, null);
    }

    <T> EmitterTransform<T> execute(final AsyncParser<T> parser, Runnable cancel) {
        assert parser != null;
        EmitterTransform<T> ret = new EmitterTransform<T>(cancel) {
            TransformFuture<T, LoaderEmitter> self = this;
            @Override
            protected void transform(LoaderEmitter emitter) throws Exception {
                super.transform(emitter);
                parser.parse(this.emitter).setCallback(new FutureCallback<T>() {
                    @Override
                    public void onCompleted(Exception e, T result) {
                        postExecute(self, e, result);
                    }
                });
            }
        };
        getLoaderEmitter(ret);
        return ret;
    }


    @Override
    public <F extends OutputStream> ResponseFuture<F> write(F outputStream, boolean close) {
        return execute(new OutputStreamDataSink(outputStream), close, outputStream);
    }

    @Override
    public <F extends OutputStream> ResponseFuture<F> write(F outputStream) {
        return execute(new OutputStreamDataSink(outputStream), true, outputStream);
    }

    @Override
    public ResponseFuture<File> write(final File file) {
        try {
            return execute(new OutputStreamDataSink(new FileOutputStream(file)), true, file, new Runnable() {
                @Override
                public void run() {
                    file.delete();
                }
            });
        }
        catch (Exception e) {
            EmitterTransform<File> ret = new EmitterTransform<File>(null);
            ret.setComplete(e);
            return ret;
        }
    }

    Multimap bodyParameters;
    @Override
    public IonRequestBuilder setBodyParameter(String name, String value) {
        if (bodyParameters == null) {
            bodyParameters = new Multimap();
            setBody(new UrlEncodedFormBody(bodyParameters));
        }
        bodyParameters.add(name, value);
        return this;
    }

    MultipartFormDataBody multipartBody;
    @Override
    public IonRequestBuilder setMultipartFile(String name, File file) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addFilePart(name, file);
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartParameter(String name, String value) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addStringPart(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder load(File file) {
        loadInternal(null, file.toURI().toString());
        return this;
    }



    String logTag;
    int logLevel;
    @Override
    public IonRequestBuilder setLogging(String tag, int level) {
        logTag = tag;
        logLevel = level;
        return this;
    }


    ArrayList<WeakReference<Object>> groups;
    @Override
    public FutureBuilder group(Object groupKey) {
        if (groups == null)
            groups = new ArrayList<WeakReference<Object>>();
        groups.add(new WeakReference<Object>(groupKey));
        return this;
    }

    String proxyHost;
    int proxyPort;
    @Override
    public IonRequestBuilder proxy(String host, int port) {
        proxyHost = host;
        proxyPort = port;
        return this;
    }


    ProgressCallback uploadProgress;
    @Override
    public Builders.Any.B uploadProgress(ProgressCallback callback) {
        uploadProgress = callback;
        return this;
    }

    ProgressBar uploadProgressBar;
    @Override
    public Builders.Any.B uploadProgressBar(ProgressBar progressBar) {
        uploadProgressBar = progressBar;
        return this;
    }

    ProgressDialog uploadProgressDialog;
    @Override
    public Builders.Any.B uploadProgressDialog(ProgressDialog progressDialog) {
        uploadProgressDialog = progressDialog;
        return this;
    }

    ProgressCallback uploadProgressHandler;
    @Override
    public Builders.Any.B uploadProgressHandler(ProgressCallback callback) {
        uploadProgressHandler = callback;
        return this;
    }

    HeadersCallback headersCallback;
    @Override
    public Builders.Any.B onHeaders(HeadersCallback callback) {
        headersCallback = callback;
        return this;
    }


	@Override
	public <T> com.koushikdutta.ion.builder.Builders.Any.F setJsonObjectBody(
			T object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ResponseFuture<T> as(Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BF<? extends BF<?>> withBitmap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ImageView> intoImageView(ImageView imageView) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public B basicAuthentication(String username, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseFuture<String> asString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public com.koushikdutta.ion.builder.Builders.Any.F setStringBody(
			String string) {
		// TODO Auto-generated method stub
		return null;
	}
}