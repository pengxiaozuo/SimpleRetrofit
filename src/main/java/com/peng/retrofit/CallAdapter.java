package com.peng.retrofit;


import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface CallAdapter<R, T> {
    Type responseType();
    T adapt(Call<R> call);
    interface Factory {
        CallAdapter<?, ?> get(Type returnType, Annotation[] animations, Retrofit retrofit);
    }
}
