package com.peng.retrofit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Retrofit {
    String baseUrl;
    Call.Factory callFactory;
    List<Converter.Factory> converterFactories;
    List<CallAdapter.Factory> callAdapterFactories;
    private static final Map<Method, ServiceMethod<?>> serviceCache = new HashMap<>();

    private Retrofit(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.converterFactories = builder.converterFactories;
        this.callAdapterFactories = builder.callAdapterFactories;
        this.callFactory = builder.callFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> tClass) {

        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class<?>[]{tClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return loadService(method).invoke(method, args);
            }
        });
    }

    public ServiceMethod<?> loadService(Method method) {
        ServiceMethod<?> service = serviceCache.get(method);
        if (service != null) {
            return service;
        }
        synchronized (serviceCache) {
            service = serviceCache.get(method);
            if (service == null) {
                service = ServiceMethod.parseAnnotations(this, method);
                serviceCache.put(method, service);
            }
        }
        return service;
    }

    <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        for (int i = 0,count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter = converterFactories.get(i).stringConverter(type,annotations,this);
        }
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(type, annotations);
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<ResponseBody, T> nextResponseBodyConverter(Type type, Annotation[] annotations) {
        for (int i = 0; i < converterFactories.size(); i++) {
            Converter<ResponseBody, ?> converter = converterFactories.get(i)
                .responseConverter(type,annotations,this);
            if (converter != null) {
                return (Converter<ResponseBody, T>) converter;
            }
        }
        return null;
    }

    <T> Converter<T, RequestBody> requestBodyConverter(
        Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(type, parameterAnnotations,methodAnnotations);
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T, RequestBody>  nextRequestBodyConverter(
        Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        for (int i = 0; i < converterFactories.size(); i++) {
            Converter<?, RequestBody> converter = converterFactories.get(i).
                requestConverter(type,parameterAnnotations,methodAnnotations,this);
            if (converter != null) {
                return (Converter<T, RequestBody>) converter;
            }
        }
        return null;
    }

    CallAdapter<?, ?> callAdapter(Type type, Annotation[] annotations) {
        return nextCallAdapter(type, annotations);
    }

    private CallAdapter<?, ?> nextCallAdapter(Type type, Annotation[] annotations) {
        for (int i = 0; i < callAdapterFactories.size(); i++) {
            CallAdapter<?,?> converter = callAdapterFactories.get(i).get(type,annotations,this);
            if (converter != null) {
                return converter;
            }
        }
        return null;
    }
    public static final class Builder {
        String baseUrl;
        okhttp3.Call.Factory callFactory;
        private List<Converter.Factory> converterFactories = new ArrayList<>();
        private List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder addCallAdapterFactory(CallAdapter.Factory factory){
            callAdapterFactories.add(factory);
            return this;
        }

        public Builder addConverterFactory(Converter.Factory factory){
            converterFactories.add(factory);
            return this;
        }

        public Retrofit build(){
            okhttp3.Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                callFactory = new OkHttpClient();
            }
            this.callFactory = callFactory;
            callAdapterFactories.add(new DefaultCallAdapterFactory());
            List<Converter.Factory> converterFactories = new ArrayList<>();
            converterFactories.add(new BuiltInConverters());
            converterFactories.addAll(this.converterFactories);
            this.converterFactories = converterFactories;
            return new Retrofit(this);
        }
    }
}
