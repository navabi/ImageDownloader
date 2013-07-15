package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.future.ResponseFuture;

import java.io.File;
import java.io.OutputStream;

/**
* Created by koush on 5/30/13.
*/ // get the result, transformed to how you want it
public interface FutureBuilder extends  ImageViewFutureBuilder {
    /**
     * Execute the request and get the result as a String
     * @return
     */
    public ResponseFuture<String> asString();



    /**
     * Use the request as a Bitmap which can then be modified and/or applied to an ImageView.
     * @return
     */
    public Builders.Any.BF<? extends Builders.Any.BF<?>> withBitmap();

    /**
     * Execute the request and write it to the given OutputStream.
     * The OutputStream will be closed upon finishing.
     * @param outputStream OutputStream to write the request
     * @return
     */
    public <T extends OutputStream> ResponseFuture<T> write(T outputStream);

    /**
     * Execute the request and write it to the given OutputStream.
     * Specify whether the OutputStream will be closed upon finishing.
     * @param outputStream OutputStream to write the request
     * @param close Indicate whether the OutputStream should be closed on completion.
     * @return
     */
    public <T extends OutputStream> ResponseFuture<T> write(T outputStream, boolean close);

    /**
     * Execute the request and write the results to a file
     * @param file File to write
     * @return
     */
    public ResponseFuture<File> write(File file);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param <T>
     * @return
     */
    public <T> ResponseFuture<T> as(Class<T> clazz);


    /**
     * Add this request to a group specified by groupKey. This key can be used in a later call to
     * Ion.cancelAll(groupKey) to cancel all the requests in the same group.
     * @param groupKey
     * @return
     */
    public FutureBuilder group(Object groupKey);
}
