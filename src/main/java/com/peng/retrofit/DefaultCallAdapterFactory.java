package com.peng.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class DefaultCallAdapterFactory implements CallAdapter.Factory {
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] animations, Retrofit retrofit) {
        return new CallAdapter<Object, Call<?>>() {
            @Override
            public Type responseType() {
                ParameterizedType type = (ParameterizedType) returnType;
                return type.getActualTypeArguments()[0];
            }

            @Override
            public Call<?> adapt(Call<Object> call) {
                return call;
            }
        };
    }
}
