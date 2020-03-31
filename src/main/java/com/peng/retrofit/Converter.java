package com.peng.retrofit;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface Converter<F, T> {

    T convert(F value) throws IOException;

    interface Factory {
        Converter<?, RequestBody> requestConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit);

        Converter<ResponseBody, ?> responseConverter(Type type,
         Annotation[] annotations, Retrofit retrofit);

        default Converter<?, String> stringConverter(Type type, Annotation[] annotations,
                                                     Retrofit retrofit) {
            return null;
        }
    }
}
