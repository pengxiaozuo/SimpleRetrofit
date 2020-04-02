package com.peng.retrofit;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 转换器
 */
public interface Converter<F, T> {

    /**
     * 把F转换为T
     */
    T convert(F value) throws IOException;

    /**
     * 转换器工厂
     */
    interface Factory {
        /**
         * 生产请求转换器，可以把其他类型转换为RequestBody
         */
        Converter<?, RequestBody> requestConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit);

        /**
         * 生产响应转换器，原始返回类型是ResponseBody，从ResponseBody转换为其他类型
         */
        Converter<ResponseBody, ?> responseConverter(Type type,
                                                     Annotation[] annotations, Retrofit retrofit);

        /**
         * 生产字符串转换器
         */
        default Converter<?, String> stringConverter(Type type, Annotation[] annotations,
                                                     Retrofit retrofit) {
            return null;
        }
    }
}
