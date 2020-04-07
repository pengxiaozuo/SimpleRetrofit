# 带你实现一个简单的Retrofit

在Android应用开发中，网络请求这一块，相信大家都比较熟悉[Retrofit](https://github.com/square/retrofit/)库，基本上都有用到过，在平常使用中有没有碰到什么疑惑和不解呢？如果能熟悉[Retrofit](https://github.com/square/retrofit/)源码，那么在使用过程中就可以做到心中有数，以不变应万变，即使出现问题，也可以很快找到问题所在，不用直接Google或stackoverflow,我们就抱着学习的目的，把Rertrofit的基本流程实现并测试，并添加了简单的`GsonConverterFactory`和`RxJavaCallAdapter`

**测试的接口一部分用的[GitHub Api](https://api.github.com/) 一部分是自己写的简单的php程序(有贴代码，下载个php的集成环境，新建个名称一样的文件，把代码复制进去，丢www目录就ok了，根目录根据集成环境不同可能会不同，也可以自己用nodejs或者java写个简单接口)**

贴上[GitHub源码](https://github.com/pengxiaozuo/SimpleRetrofit),欢迎留言讨论,原创内容，转载请保留

在代码的实现先从简单的开始一点一点完善(实现的多了后面会变的稍微复杂点);

已实现的注解：

- `@Get`
- `@Query`
- `@Path`

**暂时没有打算考虑Kotlin和挂起函数**，请求只实现了同步方式，力求简单，方法名称尽量和[Retrofit](https://github.com/square/retrofit/)保持统一

## 源码分析

下面分析一下[Retrofit](https://github.com/square/retrofit/)的整个流程

### Retrofit实例构建

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

`CallAdapter`主要用来适配从`Call<T>`返回值形式到`T<R>`形式的适配，比如从返回值是`Observable<Object>`，则需要添加一个可以从`Call<ResponseBody>`到`Observable<Object>`的适配器工厂来生成对应的适配器，如果返回值可以适配在工厂的get方法返回适配器实例即可，如果不可以处理则返回null即可

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

看下`create`方法

```java
 public <T> T create(Class<T> tClass) {
     return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class<?>[]{tClass}, new InvocationHandler() {
         @Override
         public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
             return loadService(method).invoke(method, args);
         }
     });
 }
```

直接就是一个JDK动态代理返回接口的代理对象，这个应该都看得懂

### 请求

#### 方法调用

```java
Call<ResponseBody> call = github.contributors("square", "retrofit");
```

调用接口方法，会执行动态代理的`invoke`方法，解析方法注解并构建`ServiceMethod`，构建的`ServiceMethod`实例会加入缓存，如果下次是同一个`Method`实例则可以从缓存中读取,并且读取缓存的过程是线程安全的，代码类型DCL形式应该都看得懂。

```java
new InvocationHandler() {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return loadService(method).invoke(method, args);
    }
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

`ServiceMethod`类有2个方法，`invoke`是在`Api`方法调用时候执行，`parseAnnotations`方法是在调用时候先解用来解析注解，主要是把方法的注解，参数注解，需要的`CallAdapter`等解析获取到，因为有缓存所以再次使用就不用重新解析。

`ServiceMethod`的解析主要分2步：

- 方法注解解析和方法参数注解，这一步主要是在`RequestFactory`中进行的，这些解析的到的东西也是在`RequestFactory`中持有：
  - 方法注解：确定`Url`相对地址，请求方法是否可以包含请求体，
  - 参数注解解析：拿到所有参数的`ParameterHandler`
- 返回值类型解析：拿到需要的CallAdapter，和需要返回值泛型类型需要的转换器，这一步在`ServiceMethod`的子类`HttpServiceMethod`中进行，如果不考虑`kotlin`和挂起函数我们实际拿到的`ServiceMethod`是其子类`CallAdapted`,`CallAdapted`继承的`HttpServiceMethod`

##### 方法注解解析和方法参数注解

`RequestFactory`持有解析拿到的请求地址和参数处理器等，在实际请求时候生成`Request`，实际的解析也是在`RequestFactory`中完成的

`RequestBuidler`是对`okhttp3.Request.Builder`的封装，只要是在迭代参数处理器时候将参数处理器所拿到的参数应用到`okhttp3.Request.Builder`

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

从截取的部分关键代码可以看出来`RequestFactory`主要是做2个事情：

- 解析方法注解,拿到请求方式比如`GET`,请求地址，等，如果实现了`POST`等注解还会确定`Content-Type`等
- 解析参数注解拿到每个参数的参数处理器`ParameterHandler`

下面是解析方法注解：

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

目前只是一个简单的实现所以只看下`Get`注解，这一步可以确定请求方式是`GET`,可以拿到`Get`注解中的相对地址，这要是看不懂就没有办法了。

下面是解析参数注解：

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

- 不能注解多个`Url`
- `Url`和`Path`同时使用
- `Query`啥的不能在`Url`注解前面
- 有`Url`注解就不要在方法注解上写相对地址了
- `Path`注解不能出现在`Query`注解后面
- `Field`参数注解方法注解必须有`FormUrlEncoded`
- `Part`参数注解方法注解必须有`Multipart`
- `Par`t参数注解，value为空的情况下，参数类型必须是`Par`t或者`Iterable<Part>`的实现类型，或者`Part[]`
- `Part`参数注解，value不为空的情况下，参数类型可以是requestBodyConverter可以处理的其他类型或集合和数组，但不能是`Part`类型，**不推荐**在这里写`File`添加一个file到`RequestBody`的转换器转换，`Header`缺少filename有些语言会检测不到上传文件
- `Body`参数注解方法注解不能有`FormUrlEncoded`或`Multipar`t，并且只能有一个`Body`参数注解
- ...

##### 返回值类型解析

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
private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
    Retrofit retrofit, Method method, Type responseType) {
    return retrofit.responseBodyConverter(responseType,method.getAnnotations());
}
private static <ReturnT, ResponseT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
    Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
    return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType,annotations);
}
```

主要是解析返回值拿到`CallAdapter` 和 `responseConverter` 并返回其子类实现`CallAdapted`,需要注意的是如何拿到`Converter`和`CallAdapter`,我们看下`responseBodyConverter`方法

```java
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
```

循环之前自定义添加的`Converter.Factory`和内置的集合循环调用`responseConverter`方法，如果返回不为null则认为可以处理，直接返回，也就是说和添加顺序是有关系的，如果添加了2个都可以处理同一种类型转换器，那么后面的就没有机会处理了，`requestBodyConverter` 和 `callAdapter` 方法也是同样的流程

到这里为止，方法的解析就完了，主要是拿到`ServiceMethod`,包含下面的：

- `RequestFactory` 各个参数注解的处理器，请求方法，url等
- `CallAdapter` 返回值适配器
- `responseConverter` 返回值类型转换器

接下来就是调用拿到的`ServiceMethod`执行`invoke`方法；这个方法在其子类`HttpServiceMethod`实现，其子类`CallAdapted`并没有重写,`invoke`方法就比较简单了，创建`Call`的实现类`OkHttpCall`实例，并调用`adapt`方法，`adapt`直接调用了`callAdapter.adapt`

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

```java
@Override
ReturnT adapt(Call<ResponseT> call, Object[] args) {
    //适配返回值类型
    return callAdapter.adapt(call);
}
```

#### 真实请求

这里根据返回值的不同实现也不同，Rxjava的返回值是在真实订阅时候才会真实请求，也是调用的Call.execute,默认的适配器是返回Call的实例,在我们自己调用execute时候真实请求，但是不过不过什么实现最后的实际请求都是调用的`Call`的`execute`方法，再上面的方法调用时候我们也分析了，这里实际调用的是其子类`OkHttpCall`的`execute`

触发实际请求：

```java
Response<ResponseBody> response = call.execute();
```

这个`Response`是对`okhttp3.Response`和返回值类型泛型的一个封装

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

实际上简单的代码就是`OkHttpClient.newCall(Request)`,`Request`是`RequestFactory`的`create`方法创建的，请求回来的ResponseBody会用之前解析拿到的`responseConverter`转换，并封装成`Response<T>`

看下`create`方法：

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

这里的create方法很简单，拿到一个`RequestBuilder`实例，传入请求方法请求地址，循环应用之前解析拿到的参数处理器，最后构建一个`Request`，`RequestBuilder`是对`okhttp3.Request.Builder`的封装，实际还是会应用到`okhttp3.Request.Builder`上最后调用`build`拿到`Request`

我们看下其中一个参数处理器怎么处理的：

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

就是直接调用封装的`RequestBuilder`实例添加一个查询参数，其他处理器也是大同小异，接下来我么看下`RequestBuilder`

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

实际上就是对`okhttp3.Request.Builder`的封装，没什么好说的.

```java
ResponseBody body = response.body();
String string = body.string();
System.out.println(string);
```

因为我们没有添加其他转换器工厂，所以默认只能拿到`ResponseBody`,至此所有流程结束。是否觉得很简单呢？