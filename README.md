# 简单的Retrofit实现与源码分析

在Android应用开发中，网络请求这一块呢，相信大家都比较熟悉[Retrofit](https://github.com/square/retrofit/)库，基本上都有用到过，如果熟悉[Retrofit](https://github.com/square/retrofit/)源码，那么在使用过程中就可以做到心中有数，以不变应万变，即使出现问题，也可以很快找到问题所在，不用直接Google或stackoverflow,这个项目呢，就是抱着学习的目的，把Rertrofit的基本流程实现串通，并添加了简单的`GsonConverterFactory`和`RxJavaCallAdapter`

**示例接口处理一部分用的[GitHub Api](https://api.github.com/) 一部分是自己写的简单的php程序(有贴代码)**

贴上[源码](https://github.com/pengxiaozuo/SimpleRetrofit),欢迎留言讨论

在代码的实现先从简单的开始一点一点完善;

注解目前只实现了部分：

- `@Get`
- `@Query`
- `@Path`

请求只实现了同步方式，力求简单，方法名称尽量和[Retrofit](https://github.com/square/retrofit/)保持统一

## 源码分析

下面分析一下[Retrofit](https://github.com/square/retrofit/)的整个流程

### 实例构建

使用[Retrofit](https://github.com/square/retrofit/)

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    .build();
```

我们看下简单的build方法

```java
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
```

如果没有设置`OkHttpClient`就设置一个默认的，添加一个默认的适配器工厂处理`Call<Object>`形式的返回值，添加内置的转换器工厂。

看下`CallAdapter`代码

```java
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
     */
    Type responseType();

    /**
     * 适配
     */
    T adapt(Call<R> call);

    /**
     * 工厂
     */
    interface Factory {
        /**
         * 如果可以处理则返回一个CallAdapter实例，如果处理不了返回null就行
         * @param returnType 返回值类型 即T<R>
         */
        CallAdapter<?, ?> get(Type returnType, Annotation[] animations, Retrofit retrofit);
    }
}
```

`CallAdapter`主要用来适配从`Call<T>`返回值形式到`T<R>`形式的适配，比如从返回值是`Observable<Object>`，则需要添加一个可以从`Call<ResponseBody>`到`Observable<Object>`的适配器工厂来生成对应的适配器，如果返回值可以适配在get方法返回适配器即可，如果不可以处理则返回null即可

看下默认的适配器

```java
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
```

默认的适配器工厂生成的适配器只处理返回值是`Call`的方法，并且必须是带泛型的，如果后期没有添加其他适配器工厂则Api接口返回值只能是`Call<Xxx>`

看下转换器`Converter`

```java
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
```

`Converter`很简单就是把一种类型转换成另一种类型，`Converter.Factory`可以生成3种适配器转换请求的和转换响应的和转换成字符串的，根据Type判断如果可以处理就返回`Converter`实例，如果不能处理就返回null

### Api接口创建

```java
interface GitHubApi {
        @Get("/repos/{owner}/{repo}/contributors")
        Call<ResponseBody> contributors(
            @Path("owner") String owner,
            @Path("repo") String repo
        );
    }
```



```java
GitHubApi github = retrofit.create(GitHubApi.class);
```

看下create方法

```java
 public <T> T create(Class<T> tClass) {
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class<?>[]{tClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return loadService(method).invoke(method, args);
            }
        });
    }
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
```

直接就是一个JDK动态代理返回接口的代理对象，这个应该都看得懂


### 请求

#### 方法调用

```java
Call<ResponseBody> call = github.contributors("square", "retrofit");
```

调用接口方法，会执行动态代理的invoke方法，解析方法注解并存取缓存构造请求对象

```java
abstract class ServiceMethod<T> {
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
```

ServiceMethod类是接口方法一个解析抽象，就是把方法的注解，参数注解，方法注解啥的解析完，到时候再次使用就不用重新解析，RequestFactory是用来生成Request的，实际的解析也是在RequestFactory中完成的

```java
static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
        return new RequestFactory.Builder(retrofit, method).build();
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
```

从截取的部分关键代码可以看出来，先解析方法注解在解析的参数注解

下面是解析方法注解

```java
 void parseMethodAnnotation(Annotation annotation) {
     if (annotation instanceof Get) {
         //@Get的请求方法就是GET，相当地址是注解的value方法返回值
         parseHttpMethodAndPath("GET", ((Get) annotation).value());
     }
 }
void parseHttpMethodAndPath(String httpMethod, String value) {
    this.httpMethod = httpMethod;
    this.relativeUrl = value;
}
```

因为目前只是一个简单的实现所以只看下Get注解，就是给请求方法赋值为get把注解中的相对地址拿出来

下面是解析参数注解

```java
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
```

解析方法参数注解主要是拿到参数处理器，基本每个参数注解，这里有一些需要注意的，因为我只实现了简单的几个注解，所以这里就用Retrofit来说了

- 不能注解多个Url
- Url和Path同时使用
- Query啥的不能在Url注解前面
- 有Url注解就不要在方法注解上写相对地址了
- Path注解不能出现在Query注解后面
- Field参数注解方法注解必须有FormUrlEncoded
- Part参数注解方法注解必须有Multipart
- Part参数注解，value为空的情况下，参数类型必须是Part或者`Iterable<Part>`的实现类型，或者Part[]
- Part参数注解，value不为空的情况下，参数类型可以是requestBodyConverter可以处理的其他类型或集合和数组，但不能是Part类型，不推荐在这里写File添加一个file到RequestBody的转换器转换，Header缺少filename有些语言会检测不到上传文件
- Body参数注解方法注解不能有FormUrlEncoded或Multipart，并且只能有一个Body参数注解
- ...

接下来看`HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);`方法

```java
static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
    Retrofit retrofit, Method method, RequestFactory requestFactory) {
    //获取返回值类型Type
    Type returnType = method.getGenericReturnType();
    Annotation[] annotations = method.getAnnotations();
    //获取CallAdapter
    CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter(retrofit, method, returnType, annotations);
    //获取ResponseBodyConverter
    Type responseType = callAdapter.responseType();
    Converter<ResponseBody, ResponseT> responseConverter = createResponseConverter(retrofit, method, responseType);
    okhttp3.Call.Factory callFactory = retrofit.callFactory;
    //返回子类实例
    return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
}
```

主要是解析返回值拿到CallAdapter 和 responseConverter 并返回其子类实现CallAdapted

到这里为止，方法的解析就完了，主要是拿到ServiceMethod,包含下面的：

- RequestFactory 各个参数注解的处理器，请求方法，url等
- CallAdapter 返回值适配器
- responseConverter 返回值类型转换器

接下来就是调用拿到的ServiceMethod执行invoke方法；这个方法在其子类HttpServiceMethod实现

```java
@Override
ReturnT invoke(Method method, Object[] args) {
    //创建Call的子类实现，目前只有OkHttpCall
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, responseConverter, args, callFactory);
    //适配返回值类型
    return adapt(call, args);
}
abstract ReturnT adapt(Call<ResponseT> call,Object[] args);
```

adapt方法在CallAdapted中实现的,直接调用适配器适配返回值，假设这里用的是默认适配器那么直接返回就是OkHttpCall的实例

```java
@Override
ReturnT adapt(Call<ResponseT> call, Object[] args) {
    //适配返回值类型
    return callAdapter.adapt(call);
}
```

#### 真实请求

这里根据返回值的不同实现也不同，Rxjava的返回值是在真实订阅时候才会真实请求，也是调用的Call.execute,默认的适配器是返回Call的实例我们自己调用execute

```java
Response<ResponseBody> response = call.execute();
```

这个Response是对okhttp3.Response和返回值类型泛型的一个封装
当调用execute方法时，实际调用的是其实现类OkHttpCall

```java
@Override
public Response<T> execute() throws IOException {
    return parseResponse(getRawCall().execute());
}

/**
    * okhttp3.Response -> Response<T>
    */
private Response<T> parseResponse(okhttp3.Response response) throws IOException {
    ResponseBody body = response.body();
    //转换ResponseBody -> T
    T t = responseConverter.convert(body);
    return Response.success(t, response);
}

private okhttp3.Call getRawCall() throws IOException {
    okhttp3.Call call = rawCall;
    if (call != null) return call;
    return rawCall = createRawCall();
}

private okhttp3.Call createRawCall() throws IOException {
    return callFactory.newCall(requestFactory.create(args));
}
```

实际上简单的代码就是OkHttpClient.newCall(Request)，然后converter转换，Requset是调用requestFactory.create创建的，requestFactory是在我们调用方法时候解析拿到的，我们看下create方法怎么创建的Request实例

```java
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
```

这里的create方法很简单，拿到一个封装的RequestBuilder实例，传入请求方法请求地址，循环应用之前解析拿到的参数处理器，最后构建一个Request

我们看下其中一个处理器怎么处理的

```java
static class Query<T> extends ParameterHandler<T> {

    Converter<T,String> stringConverter;
    String name;
    boolean encoded;

    Query(Converter<T, String> stringConverter, String name, boolean encoded) {
        this.stringConverter = stringConverter;
        this.name = name;
        this.encoded = encoded;
    }

    @Override
    void apply(RequestBuilder requestBuilder, T value) throws IOException {
        String queryValue = stringConverter.convert(value);
        requestBuilder.addQueryParam(name,queryValue,encoded);
    }
}
```

就是直接调用封装的RequestBuilder实例添加一个查询参数，其他处理器也是大同小异，接下来我么看下RequestBuilder

```java
class RequestBuilder {
    private final String method;
    private final String baseUrl;
    private String relativeUrl;
    private HttpUrl.Builder urlBuilder;

    private final Request.Builder requestBuilder;

    RequestBuilder(String method, String baseUrl, String relativeUrl) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.requestBuilder = new Request.Builder();
    }

    /**
     * 添加query参数 (?name=value&name1=value1)
     */
    void addQueryParam(String name, String value, boolean encoded) {
        if (relativeUrl != null) {
            urlBuilder = HttpUrl.parse(baseUrl).newBuilder(relativeUrl);
            relativeUrl = null;
        }
        if (encoded) {
            urlBuilder.addEncodedQueryParameter(name, value);
        } else {
            urlBuilder.addQueryParameter(name, value);
        }
    }

    /**
     * 替换资源路径
     */
    void addPathParam(String name, String value) {
        relativeUrl = relativeUrl.replace("{" + name + "}",value);
    }

    Request.Builder get() {
        HttpUrl url;
        HttpUrl.Builder urlBuilder = this.urlBuilder;
        if (urlBuilder != null) {
            url = urlBuilder.build();
        } else {
            url = HttpUrl.parse(baseUrl).resolve(relativeUrl);
        }
        return requestBuilder
            .url(url)
            .method(method, null);
    }
}s
```

实际上就是根据方法不同最后都是调用的okHttp的请求构建添加的参数最后构建的一个OkHttp.Request.Builder,然后调用build拿到Request,

```java
ResponseBody body = response.body();
String string = body.string();
System.out.println(string);
```

现在我们就可以拿到请求体获取body了，至此所有流程结束。