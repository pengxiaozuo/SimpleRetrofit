package com.peng.retrofit;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * 注解参数处理器
 *
 * @param <T>
 */
abstract class ParameterHandler<T> {

    abstract void apply(RequestBuilder requestBuilder, T value) throws IOException;

    static class Query<T> extends ParameterHandler<T> {

        Converter<T, String> stringConverter;
        String name;
        boolean encoded;

        Query(Converter<T, String> stringConverter, String name, boolean encoded) {
            this.stringConverter = stringConverter;
            this.name = name;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            String queryValue = stringConverter.convert(value);
            requestBuilder.addQueryParam(name, queryValue, encoded);
        }
    }

    static class Path<T> extends ParameterHandler<T> {
        String name;
        Converter<T, String> stringConverter;

        Path(String name, Converter<T, String> stringConverter) {
            this.name = name;
            this.stringConverter = stringConverter;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            requestBuilder.addPathParam(name, stringConverter.convert(value));
        }
    }

    static class Part<T> extends ParameterHandler<T> {
        String name;
        Converter<T, RequestBody> requestBodyConverter;
        String encoding;
        Headers headers;

        Part(String name, Converter<T, RequestBody> requestBodyConverter, String encoding, Headers headers) {
            this.name = name;
            this.requestBodyConverter = requestBodyConverter;
            this.encoding = encoding;
            this.headers = headers;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            RequestBody body = requestBodyConverter.convert(value);
            requestBuilder.addPart(headers,body);
        }
    }

    static class RawPart extends ParameterHandler<MultipartBody.Part> {
        static final RawPart INSTANCE = new RawPart();

        @Override
        void apply(RequestBuilder requestBuilder, MultipartBody.Part value) throws IOException {
            requestBuilder.addPart(value);
        }
    }

    static class Field<T> extends ParameterHandler<T> {
        String name;
        Converter<T, String> stringConverter;
        boolean encoded;

        Field(String name, Converter<T, String> stringConverter, boolean encoded) {
            this.name = name;
            this.stringConverter = stringConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            String v = stringConverter.convert(value);
            requestBuilder.addFormField(name, encoded, v);
        }
    }

    static class Body<T> extends ParameterHandler<T> {
        Converter<T, RequestBody> requestBodyConverter;

        Body(Converter<T, RequestBody> requestBodyConverter) {
            this.requestBodyConverter = requestBodyConverter;
        }

        @Override
        void apply(RequestBuilder requestBuilder, T value) throws IOException {
            RequestBody body = requestBodyConverter.convert(value);
            requestBuilder.setBody(body);
        }
    }
}
