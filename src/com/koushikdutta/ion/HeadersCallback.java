package com.koushikdutta.ion;

import com.koushikdutta.async.http.libcore.RawHeaders;

/**
 * Created by koush on 6/30/13.
 */
public interface HeadersCallback {
    public void onHeaders(RawHeaders headers);
}
