package com.peng.retrofit;


import java.lang.reflect.Method;

public abstract class ServiceMethod<T> {
    /**
     * 执行方法调用
     */
    abstract T invoke(Method method, Object[] args);

    /**
     * 解析注解
     */
    static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
        //解析注解生成RequestFactory
        //RequestFactory 包含已经解析好的url、请求方式、各注解的参数处理器(ParameterHandler)...
        RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);
        //这个方法调用其实是解析获取CallAdapter和ResponseBody的Converter
        //如果不考虑kotlin的挂起函数的话，返回的是HttpServiceMethod和子类CallAdapted
        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }
}
