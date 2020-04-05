package com.peng.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 内置的默认适配器
 */
class DefaultCallAdapterFactory implements CallAdapter.Factory {
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] animations, Retrofit retrofit) {
        if (Utils.getRawType(returnType) != Call.class) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)){
            throw new IllegalArgumentException("返回值必须是泛型参数类型");
        }
        return new CallAdapter<Object, Call<?>>() {
            @Override
            public Type responseType() {
                return Utils.getParameterUpperBound(0,(ParameterizedType) returnType);
            }

            @Override
            public Call<?> adapt(Call<Object> call) {
                //因为返回本身就是Call<Object>这种形式所以直接返回就好了
                return call;
            }
        };
    }
}
