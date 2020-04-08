package com.peng.retrofit;

import com.peng.retrofit.http.Body;
import com.peng.retrofit.http.Field;
import com.peng.retrofit.http.FormUrlEncoded;
import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Multipart;
import com.peng.retrofit.http.Part;
import com.peng.retrofit.http.Path;
import com.peng.retrofit.http.Post;
import com.peng.retrofit.http.Query;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

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
    boolean isFormEncoded;
    boolean isMultipart;
    RequestFactory(Builder builder) {
        this.baseUrl = builder.retrofit.baseUrl;
        this.relativeUrl = builder.relativeUrl;
        this.retrofit = builder.retrofit;
        this.method = builder.method;
        this.httpMethod = builder.httpMethod;
        this.parameterHandlers = builder.parameterHandlers;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
    }

    static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
        return new RequestFactory.Builder(retrofit, method).build();
    }

    /**
     *  在调用Call.execute方法时，需求真正的去请求了，需要构建okttp3.Request并构建okhttp3.Call执行execute
     */
    public Request create(Object[] args) throws IOException {
        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;
        RequestBuilder requestBuilder = new RequestBuilder(httpMethod, baseUrl, relativeUrl, isFormEncoded, isMultipart);
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
        boolean gotField;
        boolean gotPart;
        boolean gotBody;
        boolean hasBody;
        boolean isFormEncoded;
        boolean isMultipart;
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
            if (!hasBody) {
                if (isMultipart) {
                    throw new IllegalArgumentException("Multipart 只能用与可以携带请求体的请求方式中使用，比如POST");
                }
                if (isFormEncoded) {
                    throw new IllegalArgumentException("FormUrlEncoded 只能用与可以携带请求体的请求方式中使用，比如POST");
                }
            }
            //解析方法参数,返回相应的参数处理器实例
            int parameterCount = paramsAnnotationsArray.length;
            parameterHandlers = new ParameterHandler[parameterCount];
            for (int index = 0, last = parameterCount - 1; index < parameterCount; index++) {
                parameterHandlers[index] = parseParameter(index, parameterTypes[index], paramsAnnotationsArray[index], index == last);
            }

            if (!isMultipart && !isFormEncoded && !hasBody && gotBody) {
                throw new IllegalArgumentException("Http请求方式不能包含请求体");
            }
            if (isFormEncoded && !gotField) {
                throw new IllegalArgumentException("Field和FormUrlEncoded必须配套使用");
            }
            if (isMultipart && gotPart) {
                throw new IllegalArgumentException("Part和Multipart必须配套使用");
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
                parseHttpMethodAndPath("GET", ((Get) annotation).value(),false);
            } else if (annotation instanceof Post) {
                parseHttpMethodAndPath("POST", ((Post) annotation).value(),true);
            } else if (annotation instanceof FormUrlEncoded) {
                if (isMultipart) {
                    throw new  IllegalArgumentException("FormUrlEncoded 和 Multipart不能同时使用");
                }
                isFormEncoded = true;
            } else if (annotation instanceof Multipart) {
                if (isFormEncoded) {
                    throw new  IllegalArgumentException("FormUrlEncoded 和 Multipart不能同时使用");
                }
                isMultipart = true;
            }
        }

        /**
         * 解析请求方法和url
         */
        void parseHttpMethodAndPath(String httpMethod, String value,boolean hasBody) {
            this.httpMethod = httpMethod;
            this.relativeUrl = value;
            this.hasBody = hasBody;
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
            } else if (annotation instanceof Part) {
                if (!isMultipart) {
                    throw new IllegalArgumentException("Part必须和Multipart配套使用");
                }
                gotPart = true;
                Part part = (Part) annotation;
                String name = part.value();
                String encoding = part.encoding();
                Class<?> rawType = Utils.getRawType(type);
                if (name.isEmpty()) {
                    if (MultipartBody.Part.class.isAssignableFrom(rawType)) {
                        return ParameterHandler.RawPart.INSTANCE;
                    } else {
                        throw new IllegalArgumentException("空value的Part只可以注解MultipartBody.Part");
                    }
                } else {
                    if (MultipartBody.Part.class.isAssignableFrom(rawType)) {
                        throw new IllegalArgumentException("Part只注解MultipartBody.Part注解是不可以给value赋值");
                    }
                    Headers headers = Headers.of("Content-Disposition","from-data; name=\"" + name +"\"",
                        "Content-Transfer-Encoding",encoding);
                    Converter<?, RequestBody> requestBodyConverter =
                        retrofit.requestBodyConverter(type,annotations,methodAnnotations);
                    return new ParameterHandler.Part<>(name,requestBodyConverter,encoding,headers);
                }
            } else if (annotation instanceof Field) {
                if (!isFormEncoded) {
                    throw new IllegalArgumentException("Field必须和FormUrlEncoded配套使用");
                }
                gotField = true;
                Field field = (Field) annotation;
                boolean encoded = field.encoded();
                Converter<?, String> stringConverter = retrofit.stringConverter(type, annotations);
                return new ParameterHandler.Field<>(field.value(), stringConverter, encoded);
            } else if (annotation instanceof Body) {
                if (isFormEncoded || isMultipart) {
                    throw new IllegalArgumentException("Body参数注解无法使用from编码方式");
                }
                if (gotBody) {
                    throw new IllegalArgumentException("不能使用多个Body参数注解");
                }
                gotBody = true;
                Converter<?, RequestBody> requestBodyConverter =
                    retrofit.requestBodyConverter(type,annotations,methodAnnotations);
                return new ParameterHandler.Body<>(requestBodyConverter);
            }
            //到这里说明这个注解不是本框架的参数注解直接返回null就好了
            return null;
        }
    }
}
