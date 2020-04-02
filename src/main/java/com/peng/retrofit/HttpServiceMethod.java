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
        //创建Call的子类实现，目前只有OkHttpCall
        Call<ResponseT> call = new OkHttpCall<>(requestFactory, responseConverter, args, callFactory);
        //适配返回值类型
        return adapt(call, args);
    }

    /**
     * 返回值类型适配
     */
    abstract ReturnT adapt(Call<ResponseT> call,Object[] args);

    static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
        Retrofit retrofit, Method method, RequestFactory requestFactory) {
        //获取返回值类型Type
        Type returnType = method.getGenericReturnType();
        Annotation[] annotations = method.getAnnotations();
        //获取CallAdapter
        CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter(retrofit, method, returnType, annotations);
        //获取ResponseBodyConverter
        Type responseType = callAdapter.responseType();
        Converter<ResponseBody, ResponseT> responseConverter = createResponseConverter(retrofit, method, responseType);
        okhttp3.Call.Factory callFactory = retrofit.callFactory;
        //返回子类实例
        return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
    }

    /**
     * 获取ResponseBody的转换器
     */
    private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
        Retrofit retrofit, Method method, Type responseType) {
        return retrofit.responseBodyConverter(responseType,method.getAnnotations());
    }
    /**
     * 根据方法的返回值类型获取CallAdapter
     */
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
            //适配返回值类型
            return callAdapter.adapt(call);
        }
    }
}
