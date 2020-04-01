package com.peng.retrofit;

import java.io.IOException;

public abstract class ParameterHandler<T> {

    abstract void apply(RequestBuilder requestBuilder, T value) throws IOException;

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
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            String queryValue = stringConverter.convert(value);
            requestBuilder.addQueryParam(name,queryValue,encoded);
        }
    }

    static class Path<T> extends ParameterHandler<T> {
        String name;
        Converter<T,String> stringConverter;

        public Path(String name, Converter<T, String> stringConverter) {
            this.name = name;
            this.stringConverter = stringConverter;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            requestBuilder.addPathParam(name,stringConverter.convert(value));
        }
    }
}
