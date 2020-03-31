package com.peng.retrofit;

import java.io.IOException;

public abstract class ParameterHandler<T> {

    abstract void apply(RequestBuilder requestBuilder, T value);

    static class Query<T> extends ParameterHandler<T> {

        Converter<T,String> stringConverter;
        String name;
        boolean encoded;

        public Query(Converter<T, String> stringConverter, String name, boolean encoded) {
            this.stringConverter = stringConverter;
            this.name = name;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) {
            String queryValue = null;
            try {
                queryValue = stringConverter.convert(value);
            } catch (IOException e) {
                e.printStackTrace();
            }
            requestBuilder.addQueryParam(name,queryValue,encoded);
        }
    }
}
