package com.peng.retrofit;

import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Path;
import com.peng.retrofit.http.Query;
import okhttp3.Request;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Request create(Object[] args) throws IOException {
        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;
        RequestBuilder requestBuilder = new RequestBuilder(httpMethod, baseUrl, relativeUrl);
        int argumentCount = args.length;
        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(requestBuilder, args[p]);
        }
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
        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.paramsAnnotationsArray = method.getParameterAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
        }

        RequestFactory build() {
            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }
            int parameterCount = paramsAnnotationsArray.length;
            parameterHandlers = new ParameterHandler[parameterCount];
            for (int index = 0, last = parameterCount - 1; index < parameterCount; index++) {
                parameterHandlers[index] = parseParameter(index, parameterTypes[index], paramsAnnotationsArray[index], index == last);
            }

            return new RequestFactory(this);
        }

        void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof Get) {
                parseHttpMethodAndPath("GET", ((Get) annotation).value());
            }
        }

        void parseHttpMethodAndPath(String httpMethod, String value) {
            this.httpMethod = httpMethod;
            this.relativeUrl = value;
        }

        ParameterHandler<?> parseParameter(int index, Type type, Annotation[] annotationArray, boolean allowContinuation) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotationArray) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(index, type, annotationArray, annotation);
                if (annotationAction == null) continue;
                result = annotationAction;
            }
            return result;
        }

        ParameterHandler<?> parseParameterAnnotation(int index, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Query) {
                Query query = (Query) annotation;
                String name = query.value();
                boolean encoded = query.encoded();
                Converter<?, String> stringConverter = retrofit.stringConverter(type,annotations);
                return new ParameterHandler.Query<>(stringConverter, name, encoded);
            } else if (annotation instanceof Path) {
                Path path = (Path) annotation;
                Converter<?, String> stringConverter = retrofit.stringConverter(type, annotations);
                String name = path.value();
                return new ParameterHandler.Path<>(name, stringConverter);
            }
            return null;
        }
    }
}
