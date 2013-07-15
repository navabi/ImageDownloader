package com.koushikdutta.ion;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.util.ArrayList;

class BitmapCallback {
    String key;
    Ion ion;

    public BitmapCallback(Ion ion, String key, boolean put) {
        this.key = key;
        this.put = put;
        this.ion = ion;
    }

    boolean put;

    boolean put() {
        return put;
    }

    void report(final Exception e, final BitmapInfo info) {
        AsyncServer.post(Ion.mainHandler, new Runnable() {
            @Override
            public void run() {
                BitmapInfo result = info;
                if (result == null) {
                    // cache errors
                    result = new BitmapInfo();
                    result.bitmap = null;
                    result.key = key;
                    ion.getBitmapCache().put(result);
                } else if (put()) {
                    ion.getBitmapCache().put(result);
                }

                final ArrayList<FutureCallback<BitmapInfo>> callbacks = ion.bitmapsPending.remove(key);
                if (callbacks == null || callbacks.size() == 0)
                    return;

                for (FutureCallback<BitmapInfo> callback : callbacks) {
                    callback.onCompleted(e, result);
                }
            }
        });
    }
}
