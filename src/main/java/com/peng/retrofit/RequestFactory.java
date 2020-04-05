package com.peng.retrofit;

import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Path;
import com.peng.retrofit.http.Query;
import okhttp3.Request;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class RequestFactory {

    String baseUrl;
    String relativeUrl;
    Retrofit retrofit;
    Method method;
    String httpMethod;
    ParameterHandler<?>[] parameterHandlers;

    RequestFactory(Builder builder) {
        this.baseUrl = builder.retrofit.baseUrl;
        this.relativeUrl = builder.relativeUrl;
        this.retrofit = builder.retrofit;
        this.method = builder.method;
        this.httpMethod = builder.httpMethod;
        this.parameterHandlers = builder.parameterHandlers;
    }

    static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
        return new RequestFactory.Builder(retrofit, method).build();
    }

    /**
     *  在调用Call.execute方法时，需求真正的去请求了，需要构建okttp3.Request并构建okhttp3.Call执行execute
     */
    public Request create(Object[] args) throws IOException {
        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;
        RequestBuilder requestBuilder = new RequestBuilder(httpMethod, baseUrl, relativeUrl);
        int argumentCount = args.length;
        //循环应用参数处理器
        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(requestBuilder, args[p]);
        }
        //构建Request
        return requestBuilder.get().build();
    }

    static class Builder {
        Retrofit retrofit;
        Method method;
        Annotation[] methodAnnotations;
        Annotation[][] paramsAnnotationsArray;
        Type[] parameterTypes;
        ParameterHandler<?>[] parameterHandlers;
        String relativeUrl;
        String httpMethod;
        boolean gotQuery;
        boolean gotPath;
        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.paramsAnnotationsArray = method.getParameterAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
        }

        RequestFactory build() {
            //解析方法注解
            //@Get 会拿到请求方法 相对的请求地址
            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }
            //解析方法参数,返回相应的参数处理器实例
            int parameterCount = paramsAnnotationsArray.length;
            parameterHandlers = new ParameterHandler[parameterCount];
            for (int index = 0, last = parameterCount - 1; index < parameterCount; index++) {
                parameterHandlers[index] = parseParameter(index, parameterTypes[index], paramsAnnotationsArray[index], index == last);
            }

            return new RequestFactory(this);
        }

        /**
         * 解析方法注解
         * @param annotation
         */
        void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof Get) {
                //@Get的请求方法就是GET，相当地址是注解的value方法返回值
                parseHttpMethodAndPath("GET", ((Get) annotation).value());
            }
        }

        /**
         * 解析请求方法和url
         */
        void parseHttpMethodAndPath(String httpMethod, String value) {
            this.httpMethod = httpMethod;
            this.relativeUrl = value;
        }

        /**
         * 解析方法参数注解并返回对应的参数处理器
         */
        ParameterHandler<?> parseParameter(int index, Type type, Annotation[] annotationArray, boolean allowContinuation) {
            ParameterHandler<?> result = null;
            //方法参数上可能有多个注解，Retrofit只解析自己的
            for (Annotation annotation : annotationArray) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(index, type, annotationArray, annotation);
                //如果返回null说明不是Retrofit注解，如果找到了则直接返回，没有一个参数有多个Retrofit注解的情况
                if (annotationAction == null) continue;
                result = annotationAction;
            }
            return result;
        }
        /**
         * 解析方法参数注解并返回对应的参数处理器
         */
        ParameterHandler<?> parseParameterAnnotation(int index, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Query) {
                gotQuery = true;
                //如果是Query注解，则需要拿到参数名 和对应的参数值转换器
                Query query = (Query) annotation;
                String name = query.value();
                boolean encoded = query.encoded();
                //query String 是 ?name=value&name1=value1 所以需要StringConverter
                Converter<?, String> stringConverter = retrofit.stringConverter(type, annotations);
                return new ParameterHandler.Query<>(stringConverter, name, encoded);
            } else if (annotation instanceof Path) {
                if (gotQuery) {
                    throw new IllegalArgumentException("Path注解不能在Query注解后面");
                }
                gotPath = true;
                //如果是Path注解，则需要拿到参数名 和对应的参数值转换器
                Path path = (Path) annotation;
                //资源路径是url中所以也是需要StringConverter
                Converter<?, String> stringConverter = retrofit.stringConverter(type, annotations);
                String name = path.value();
                return new ParameterHandler.Path<>(name, stringConverter);
            }
            //到这里说明这个注解不是本框架的参数注解直接返回null就好了
            return null;
        }
    }
}
