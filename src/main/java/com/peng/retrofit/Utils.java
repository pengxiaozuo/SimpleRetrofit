package com.peng.retrofit;

import java.lang.reflect.*;

public class Utils {

    /**
     *  获取泛型参数的上界类型
     *  <Object> -> Object
     *  <? extends Object> -> Object
     */
    static Type getParameterUpperBound(int index, ParameterizedType type) {
        //获取泛型参数类型数组
        Type[] types = type.getActualTypeArguments();
        Type t = types[index];
        //如果是通配符类型则获取上界
        if (t instanceof WildcardType) {
            t = ((WildcardType) t).getUpperBounds()[0];
        }
        return t;
    }

    /**
     * 获取原始类型
     * Class<?> -> Class<?>
     * List<Object> -> List.class
     * Object[] -> [LObject.class
     * T -> Object.class
     * List<? extends Object> -> List.class
     */
     static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        //如果是通配符类型则获取上界
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<?>) parameterizedType.getRawType();
        }
        //如果是数组类型
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType),0).getClass();
        }
        //如果是变量类型的 比如T
        if (type instanceof TypeVariable) {
            return Object.class;
        }
        //如果是通配符类型比如<? extends Object>
        if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }
        throw new IllegalArgumentException("参数错误");
    }
}
