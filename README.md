# 实现一个简单的Retrofit

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

### Api接口创建

### 请求
