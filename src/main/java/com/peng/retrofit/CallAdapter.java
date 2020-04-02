package com.peng.retrofit;


import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 返回值适配器
 * 原始的返回值只能是Call<ResponseBody>这种形式(挂起函数啥的先不算，简单的来说就这样)
 * CallAdapter的作用基本可以简单的理解为把Call<ResponseBody>(Call<R>) 适配为T<R> 比如：Observable<ResponseBody>
 * @param <R> 返回值泛型参数类型 ，如果返回值是Observable<ResponseBody> 则R是ResponseBody
 * @param <T> 返回值的类型 Observable<ResponseBody> 则T是Observable
 */
public interface CallAdapter<R, T> {
    /**
     * 返回值的泛型类型
     * @return
     */
    Type responseType();

    /**
     * 适配
     * @param call
     * @return
     */
    T adapt(Call<R> call);

    /**
     * 工厂
     */
    interface Factory {
        /**
         * 如果可以处理则返回一个CallAdapter实例，如果处理不了返回null就行
         * @param returnType 返回值类型 即T<R>
         * @param animations
         * @param retrofit
         * @return
         */
        CallAdapter<?, ?> get(Type returnType, Annotation[] animations, Retrofit retrofit);
    }
}
