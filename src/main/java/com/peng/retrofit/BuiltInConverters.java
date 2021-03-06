package com.peng.retrofit;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 内置的转化器工厂
 */
class BuiltInConverters implements Converter.Factory {


    @Override
    public Converter<?, RequestBody> requestConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
            return RequestBodyConverter.INSTANCE;
        }
        return null;
    }

    @Override
    public Converter<ResponseBody, ?> responseConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        //如果返回值类型是ResponseBody则用BufferingResponseBodyConverter
        if (type == ResponseBody.class) {
            return BufferingResponseBodyConverter.INSTANCE;
        }
        return null;
    }

    static final class ToStringConverter implements Converter<Object,String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();
        @Override
        public String convert(Object value) {
            return value.toString();
        }
    }

    static final class BufferingResponseBodyConverter
        implements Converter<ResponseBody, ResponseBody> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override public ResponseBody convert(ResponseBody value) throws IOException {
            try {
                Buffer buffer = new Buffer();
                value.source().readAll(buffer);
                return ResponseBody.create(value.contentType(), value.contentLength(), buffer);
            } finally {
                value.close();
            }
        }
    }
    static final class RequestBodyConverter implements Converter<RequestBody, RequestBody> {
        static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

        @Override public RequestBody convert(RequestBody value) {
            return value;
        }
    }
}
