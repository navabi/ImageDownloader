package com.koushikdutta.ion.builder;

import android.widget.ImageView;

import com.koushikdutta.async.future.Future;

/**
* Created by koush on 5/30/13.
*/
public interface ImageViewFutureBuilder {
    /**
     * Perform the request and get the result as a Bitmap, which will then be loaded
     * into the given ImageView
     * @param imageView ImageView to set once the request completes
     * @return
     */
    public Future<ImageView> intoImageView(ImageView imageView);
}
