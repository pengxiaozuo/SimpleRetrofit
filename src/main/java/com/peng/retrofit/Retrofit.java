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
import java.util.*;

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

    /**
     * 根据传入的接口class生成动态代理实例
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> tClass) {

        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class<?>[]{tClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return loadService(method).invoke(method, args);
            }
        });
    }

    /**
     * 加载ServiceMethod
     */
    private ServiceMethod<?> loadService(Method method) {
        //如果缓存有，则从缓存中读取
        ServiceMethod<?> service = serviceCache.get(method);
        if (service != null) {
            return service;
        }
        synchronized (serviceCache) {
            //缓存中没有则解析方法生成ServiceMethod加入缓存并返回
            service = serviceCache.get(method);
            if (service == null) {
                service = ServiceMethod.parseAnnotations(this, method);
                serviceCache.put(method, service);
            }
        }
        return service;
    }

    /**
     * 获取  T->String的Converter
     */
    @SuppressWarnings("unchecked")
    <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        //先从几种中找，如果集中没有则返回内置，如果找到了则返回
        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter = converterFactories.get(i).stringConverter(type, annotations, this);
            //只要工厂get方法返回不是null就当他返回了可以处理的Converter，直接返回，后面的就没有机会了
            if (converter != null) {
                return (Converter<T, String>) converter;
            }
        }
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    /**
     * 获取ResponseBody->T的转换器
     */
    <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(type, annotations);
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<ResponseBody, T> nextResponseBodyConverter(Type type, Annotation[] annotations) {
        //获取ResponseBody->T的转换器，如果找到直接返回，没找到抛异常
        for (int i = 0; i < converterFactories.size(); i++) {
            Converter<ResponseBody, ?> converter = converterFactories.get(i)
                .responseConverter(type, annotations, this);
            //只要工厂get方法返回不是null就当他返回了可以处理的Converter，直接返回，后面的就没有机会了
            if (converter != null) {
                return (Converter<ResponseBody, T>) converter;
            }
        }
        throw new IllegalArgumentException("没有找到可以处理的ResponseBodyConverter");
    }

    /**
     * 获取 T->RequestBody的转换器
     */
    <T> Converter<T, RequestBody> requestBodyConverter(
        Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(type, parameterAnnotations, methodAnnotations);
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T, RequestBody> nextRequestBodyConverter(
        Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        //获取T->RequestBody的转换器，如果找到直接返回，没找到抛异常
        for (int i = 0; i < converterFactories.size(); i++) {
            Converter<?, RequestBody> converter = converterFactories.get(i).
                requestConverter(type, parameterAnnotations, methodAnnotations, this);
            //只要工厂get方法返回不是null就当他返回了可以处理的Converter，直接返回，后面的就没有机会了
            if (converter != null) {
                return (Converter<T, RequestBody>) converter;
            }
        }
        throw new IllegalArgumentException("没有找到可以处理的RequestBodyConverter");
    }

    /**
     * 获取适配器
     */
    CallAdapter<?, ?> callAdapter(Type type, Annotation[] annotations) {
        return nextCallAdapter(type, annotations);
    }

    private CallAdapter<?, ?> nextCallAdapter(Type type, Annotation[] annotations) {
        for (int i = 0; i < callAdapterFactories.size(); i++) {
            CallAdapter<?, ?> converter = callAdapterFactories.get(i).get(type, annotations, this);
            //只要工厂get方法返回不是null就当他返回了可以处理的Converter，直接返回，后面的就没有机会了
            if (converter != null) {
                return converter;
            }
        }
        throw new IllegalArgumentException("没有找到可以处理的CallAdapter");
    }

    public static final class Builder {
        String baseUrl;
        okhttp3.Call.Factory callFactory;
        private List<Converter.Factory> converterFactories = new ArrayList<>();
        private List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();

        /**
         * 设置基础的url
         * @param baseUrl
         * @return
         */
        public Builder baseUrl(String baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl == null");
            if (!baseUrl.endsWith("/")) {
                throw new IllegalArgumentException("baseUrl必须以/结尾");
            }
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * 添加一个CallAdapter.Factory 生产可处理方法返回值类型rowType的CallAdapter
         * @param factory
         * @return
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(factory);
            return this;
        }

        /**
         * 添加一个Converter.Factory 生产一个可以处理类型转换的Converter
         * @param factory
         * @return
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(factory);
            return this;
        }

        public Retrofit build() {
            //如果没有设置callFactory 则创建一个默认的OkHttpClient
            okhttp3.Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                callFactory = new OkHttpClient();
            }
            this.callFactory = callFactory;
            //在集合最后添加一个默认的返回值适配处理工厂
            // 可以处理Call<Object> 或者 Call<? extends Object>类型的适配器
            // Call<Object> 中的Object如果没有添加其他转换器工厂的话默认是okhttp3.ResponseBody
            callAdapterFactories.add(new DefaultCallAdapterFactory());
            //在集合最开始的位置添加内置的类型转换器工厂
            //内置的处类型转换工厂可以返回处理RequestBody->RequestBody和ResponseBody->ResponseBody和Object->String的类型
            //如果不添加内置的工厂，在参数处理时候会找不到转换器抛出异常
            List<Converter.Factory> converterFactories = new ArrayList<>();
            converterFactories.add(new BuiltInConverters());
            converterFactories.addAll(this.converterFactories);
            this.converterFactories = converterFactories;
            return new Retrofit(this);
        }
    }
}
