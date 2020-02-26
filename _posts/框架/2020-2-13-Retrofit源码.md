---
layout:     post
title:      "Retrofit源码解析"
subtitle:   " Retrofit源码解析"
date:       2020-02-13 22:41:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - 框架
---


### Retrofit源码解析

#### 使用示例：
```
public interface GitHub {
    @GET("/repos/{owner}/{repo}/contributors")
    Call<List<Contributor>> contributors(
        @Path("owner") String owner,
        @Path("repo") String repo);
  }

  public static void main(String... args) throws IOException {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    GitHub github = retrofit.create(GitHub.class);
    Call<List<Contributor>> call = github.contributors("square", "retrofit");
    List<Contributor> contributors = call.execute().body();
    for (Contributor contributor : contributors) {
      System.out.println(contributor.login + " (" + contributor.contributions + ")");
    }
  }
 ```


#### Retrofit.Builder().build方法：
```
public Retrofit build() {
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }

    //创建一个CallFactory、Retrofit现在默认支持okhttp3加载，该工厂用于将我们的接口方法组装成一个OkHttp请求。
      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        //传入当前线程作为回调的线程，一般这个platform.defaultCallbackExecutor()在Android平台会返回一个主线程的handle用于回调到主线程使用。
        callbackExecutor = platform.defaultCallbackExecutor();
      }

      // 保护性拷贝并添加我们使用addCallAdapterFactory方法传入的工厂类
      List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
      //如果callbackExecutor为空的话，那么这个线程的执行结果会回调给调用这个任务的线程执行，如果执行的是子线程的话可能会出错。
      callAdapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories =
          new ArrayList<>(1 + this.converterFactories.size());

      //首先添加默认的转换器，用于将请求体或响应体转换为String输出
      converterFactories.add(new BuiltInConverters());
      //添加我们自己设置的转换器，通过addConverterFactory方法添加，一般是GsonConverterFactory，为了将json格式的字符串转换为对象
      converterFactories.addAll(this.converterFactories);

      return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
          unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
    }
  }
```
在Builder的build方法中主要是

#### Retrofit的create方法：
```
 public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    //判断是否需要一次性反射加载所有方法、该属性通过Retrofit的Builder的
validateEagerly设置
    if (validateEagerly) {
       //该方法会遍历传入的模板类的方法将其一次性全添加到缓存中
      eagerlyValidateMethods(service);
    }
    
    //动态代理返回一个代理类
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            //Object父类方法不拦截
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            //不一次性拦截所有方法、该方法在Android 平台默认为false，也就是不会一次性加载所有方法。
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.adapt(okHttpCall);
          }
        });
  }
```
首先使用Retrofit时需要一个接口请求类，在调用Retrofit时会使用动态代理这个类，即调用这个类的所有方法都将进入 InvocationHandler的 invoke方法，在获取的过程中会先判断是否一次性加载所有方法，因为反射性能不好，所以需要将反射获得的方法储存起来。在create中，最终执行的方法是由loadServiceMethod返回的。

#### Retrofit的loadServiceMethod方法
```
ServiceMethod<?, ?> loadServiceMethod(Method method) {  
    //缓存获取
    ServiceMethod<?, ?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        //构建一个新的反射方法
        result = new ServiceMethod.Builder<>(this, method).build();
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
```
ServiceMethod该类是一个包装类，只是对Method的一个包装，将Method中的注解保存了下来并用它来生成了一个OkHttpCall用于构建请求。

#### ServiceMethod.adapt方法：
回到Retrofit的create的最后，返回的是一个callAdapter的adapter方法构建的adapt对象：
```
T adapt(Call<R> call) {  return callAdapter.adapt(call);}
```
回到ServiceMethod.Builder的build方法：
```
 public ServiceMethod build() {
       //构建callAdapter 
      callAdapter = createCallAdapter();
      responseType = callAdapter.responseType();
      if (responseType == Response.class || responseType == okhttp3.Response.class)       {
        throw methodError("'"
            + Utils.getRawType(responseType).getName()
            + "' is not a valid response body type. Did you mean ResponseBody?");
      }
      //构建一个响应转化器
      responseConverter = createResponseConverter();
     //解析注解 检测抛出异常
      ...
    }
```
调用的callAdapter使用createCallAdapter构建的
```
 private CallAdapter<T, R> createCallAdapter() {
      //判断注解条件是否合法
      ...
      try {
        //返回CallAdapter
        return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
      } catch (RuntimeException e) { // Wide exception range because factories are user code.
        throw methodError(e, "Unable to create call adapter for %s", returnType);
      }
    }
```
最终调用生成CallAdapter的是Retrofit的callAdapter方法：

#### Retrofit的callAdapter方法：
```
    public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }
```
调用的是nextCallAdpter方法。
```
 public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
      Annotation[] annotations) {
      //检查注解
    checkNotNull(returnType, "returnType == null");
    checkNotNull(annotations, "annotations == null");

    int start = callAdapterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
      //遍历callAdapterFactories工厂直到找到可以转换出的返回类型的工厂
      CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
      if (adapter != null) {
        return adapter;
      }
    }
    //找不到可以最终转换成的返回类型的工厂输出日志抛出异常。
    ···
  }
```
遍历callAdapterFactories这个List，在Retrofit的构造函数中对其添加了我们使用addCallAdapterFactory方法传入的工厂类，并且添加了有两个默认的CallAdapterFactory
：ExecutorCallAdapterFactory和DefaultCallAdapterFactory的其中一个。在Android平台上默认是ExecutorCallAdapterFactory，回到Retrofit的create方法中的最后一行代码，按照实例代码对应的执行应该是ExecutorCallAdapterFactory调用get方法创建的CallAdapter对象后的adapt方法。

#### ExecutorCallAdapterFactory的get方法：
```
public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Call.class) {
            return null;
        } else {
            final Type responseType = Utils.getCallResponseType(returnType);
            return new CallAdapter<Object, Call<?>>() {
                public Type responseType() {
                    return responseType;
                }

                //adapt方法
                public Call<Object> adapt(Call<Object> call) {
                    return new ExecutorCallAdapterFactory.ExecutorCallbackCall(ExecutorCallAdapterFactory.this.callbackExecutor, call);
                }
            };
        }
    }
```
其中adapt方法中返回了
```
ExecutorCallAdapterFactory.ExecutorCallbackCall(ExecutorCallAdapterFactory.this.callbackExecutor, call);
```
这行代码返回了一个ExecutorCallbackCall对象，该对象就是我们实际操作的返回值。
```
static final class ExecutorCallbackCall<T> implements Call<T> {
        final Executor callbackExecutor;
        final Call<T> delegate;

        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
        }

        public void enqueue(final Callback<T> callback) {
            Utils.checkNotNull(callback, "callback == null");
            this.delegate.enqueue(new Callback<T>() {
                public void onResponse(Call<T> call, final Response<T> response) {
                    ExecutorCallbackCall.this.callbackExecutor.execute(new Runnable() {
                        public void run() {
                            if (ExecutorCallbackCall.this.delegate.isCanceled()) {
                                callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
                            } else {
                                callback.onResponse(ExecutorCallbackCall.this, response);
                            }

                        }
                    });
                }

                public void onFailure(Call<T> call, final Throwable t) {
                    ExecutorCallbackCall.this.callbackExecutor.execute(new Runnable() {
                        public void run() {
                            callback.onFailure(ExecutorCallbackCall.this, t);
                        }
                    });
                }
            });
        }
        ...
    }
```
因此在最后我们可以调用所写的接口方法返回的Call<T>实例的enqueue方法进行异步任务操作，其中的的回调会由在Retrofit中创建的Executor中调用、也就是MainThreadExecutor调用handle将回调回传到UI线程。
可以看到ExecutorCallbackCal类只是对Call的一个装饰模式，实际发起请求的还是Call类，因为这是Okhttp的请求，所以Retrofit也只是对OkHttp做了一层封装。


#### Retrofit与Rxjava2：

- 在Retrofit中使用Rxjava2，最终会将Call对象转换为一个Observable对象，在上面我们可以看到最终生成的Call对象是由一个CallAdapterFactory生成的，而CallAdapterFactory在Retrofit的build中，在使用Rxjava2去初始化时会调用：
```
addCallAdapterFactory(RxJava2CallAdapterFactory.create())
```
根据调用的顺序，我们自己传入的CallAdapterFactory会先添加在前面，因此在遍历Retrofit的callAdapterFactories这个List时会先调用设置的RxJava2CallAdapterFactory（也只有这个类能处理返回值为Observable）。
进入RxJava2CallAdapterFactory的get方法：
```
 @Override
  public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
           ...
          //条件判断 、Rxjava的参数配置、如线程切换等
  
        return new RxJava2CallAdapter(Void.class, scheduler, isAsync, false, true, false, 
       ...
    }
```
最终正常返回的是一个RxJava2CallAdapter类，在RxJava2CallAdapter的adapt方法中：
```
 @Override public Object adapt(Call<R> call) {
    //根据是否异步执行返回不同的Observable
    Observable<Response<R>> responseObservable = isAsync
        ? new CallEnqueueObservable<>(call)
        : new CallExecuteObservable<>(call);

    Observable<?> observable;
    if (isResult) {
      //相应Observable
      observable = new ResultObservable<>(responseObservable);
    } else if (isBody) {
      //请求Observable
      observable = new BodyObservable<>(responseObservable);
    } else {
      observable = responseObservable;
    }
    //Observable参数配置
    return observable;
    ...
  }
```
//其中不同的Observable类为CallEnqueueObservable、CallExecuteObservable、ResultObservable、BodyObservable。
根据Rxjava2的中的Observable类，在具体subscribe时会调用subscribeActual方法
进入CallEnqueueObservable的subscribeActual方法：
```
	@Override protected void subscribeActual(Observer<? super Response<T>> observer) {
		//由于Call是一种一次性类型，因此请为每个新的观察者克隆它。
		Call<T> call = originalCall.clone();
		//创建一个请求CallCallback，该类实现了Callback<T>接口，又实现了Rxjava2的Disposable接口
		CallCallback<T> callback = new CallCallback<>(call, observer);
		//同步操作，也就是在使用这个observer类取消时，CallCallback这个Disposable子类也会调用相应的取消方法。
		observer.onSubscribe(callback);
		//异步请求启动
		call.enqueue(callback);
	}
```
最终还是调用了Call<T>类的enqueue方法，这个方法的回调会回到Callback的onResonse方法中：
```
	@Override public void onResponse(Call<T> call, Response<T> response) {
      if (call.isCanceled()) return;
      try {
        observer.onNext(response);
        if (!call.isCanceled()) {
          terminated = true;
          observer.onComplete();
        }
      } catch (Throwable t) {
        if (terminated) {
          RxJavaPlugins.onError(t);
        } else if (!call.isCanceled()) {
          try {
            observer.onError(t);
          } catch (Throwable inner) {
            Exceptions.throwIfFatal(inner);
            RxJavaPlugins.onError(new CompositeException(t, inner));
          }
        }
      }
    }
```
因此我们可以直接就在该会调用调用传入的Overable的对应方法。还有一点注意的是 
    observer.onSubscribe(callback);该方法在我们操作observer也会同步调用到Call中Callback的方法，这样我们也可以在CallCallback中调用Call<T>的方法来达到让请求停止等操作。

