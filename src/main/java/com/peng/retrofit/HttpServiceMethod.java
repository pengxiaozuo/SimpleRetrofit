package com.peng.retrofit;


import okhttp3.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
    private final RequestFactory requestFactory;
    private final okhttp3.Call.Factory callFactory;
    private final Converter<ResponseBody, ResponseT> responseConverter;

    HttpServiceMethod(RequestFactory requestFactory, okhttp3.Call.Factory callFactory,
                      Converter<ResponseBody, ResponseT> responseConverter) {
        this.requestFactory = requestFactory;
        this.callFactory = callFactory;
        this.responseConverter = responseConverter;
    }
    @Override
    ReturnT invoke(Method method, Object[] args) {
        Call<ResponseT> call = new OkHttpCall<>(requestFactory,responseConverter,args,callFactory);
        return adapt(call,args);
    }
    abstract ReturnT adapt(Call<ResponseT> call,Object[] args);

    static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
        Retrofit retrofit, Method method, RequestFactory requestFactory) {

        Type returnType = method.getGenericReturnType();
        Annotation[] annotations = method.getAnnotations();
        CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter(retrofit, method, returnType, annotations);
        Type responseType = callAdapter.responseType();
        Converter<ResponseBody, ResponseT> responseConverter = createResponseConverter(retrofit, method, responseType);
        okhttp3.Call.Factory callFactory = retrofit.callFactory;
        return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
    }

    private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
        Retrofit retrofit, Method method, Type responseType) {
        return retrofit.responseBodyConverter(responseType,method.getAnnotations());
    }

    @SuppressWarnings("unchecked")
    private static <ReturnT, ResponseT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
        Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
        return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType,annotations);
    }

    private static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {
        CallAdapter<ResponseT, ReturnT> callAdapter;
        public CallAdapted(RequestFactory requestFactory,
                           okhttp3.Call.Factory callFactory,
                           Converter<ResponseBody, ResponseT> responseConverter,
                           CallAdapter<ResponseT, ReturnT> callAdapter) {
            super(requestFactory,callFactory,responseConverter);
            this.callAdapter = callAdapter;
        }

        @Override
        ReturnT adapt(Call<ResponseT> call, Object[] args) {
            return callAdapter.adapt(call);
        }
    }
}
