package com.peng.retrofit;


import java.lang.reflect.Method;

public abstract class ServiceMethod<T> {
    abstract T invoke(Method method, Object[] args);

    static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
        RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }
}
