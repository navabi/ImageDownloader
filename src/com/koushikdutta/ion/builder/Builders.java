package com.koushikdutta.ion.builder;

/**
 * Created by koush on 6/10/13.
 */
public interface Builders {

    public interface ImageView {
        public interface F<A extends F<?>> extends ImageViewBuilder<A>,  LoadImageViewFutureBuilder {
        }
    }

    public interface Any {
        public interface IF<A extends IF<?>> extends ImageViewBuilder<A>, ImageViewFutureBuilder {
        }

        public interface BF<A extends BF<?>> extends  IF<A> {
        }

        public interface F extends FutureBuilder, ImageViewFutureBuilder {
        }

        public interface M extends MultipartBodyBuilder<M>, F {
        }

        public interface U extends UrlEncodedBuilder<U>, F {
        }

        public interface B extends RequestBuilder<F, B, M, U>, F {
        }
    }
}
