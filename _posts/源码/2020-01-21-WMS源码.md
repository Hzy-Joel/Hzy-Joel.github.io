---
layout:     post
title:      "WMS源码分析"
subtitle:   " WMS源码分析"
date:       2020-01-21 16:57:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - 源码
    - WMS
---



#### wms源码解析


![WMS源码UML图](https://hzy-joel.github.io/img/post/post_wms.jpg)

##### 1、Dialog中获得window并关联的实现（该方法在Dialog的构造函数中）：
```
Dialog(@NonNull Context context, @StyleRes int themeResId, boolean createContextThemeWrapper) {
      ...
      //获得WMS服务-->通过单例获取，这里不需要知道原先的服务是如何创建的，因为后面会生成一个与当前窗口关联的WindowManager对象。
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        //创建一个当前上下文对应的窗口
        final Window w = new PhoneWindow(mContext);
        mWindow = w;
        w.setCallback(this);
        w.setOnWindowDismissedCallback(this);
        w.setOnWindowSwipeDismissedCallback(() -> {
            if (mCancelable) {
                cancel();
            }
        });
        //关联当前窗口和WMS
        w.setWindowManager(mWindowManager, null, null);
        w.setGravity(Gravity.CENTER);

        mListenersHandler = new ListenersHandler(this);
    }

```
**其中最重要的是w.setWindowManager(mWindowManager, null, null)，该实现在Window中，实现了当前窗口和WMS的关联**

##### 2、关联当前窗口和WMS的方法-->window的setWindowManager方法：
```
 public void setWindowManager(WindowManager wm, IBinder appToken, String appName,
            boolean hardwareAccelerated) {
        mAppToken = appToken;
        mAppName = appName;
        mHardwareAccelerated = hardwareAccelerated
                || SystemProperties.getBoolean(PROPERTY_HARDWARE_UI, false);
        if (wm == null) {
            wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        }
        //通过该方法创建了一个和当前窗口关联的WindowManager对象，WindowManagerImpl为具体实现。
        mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
    }

```
**该方法最后调用了WindowManagerImpl的createLocalWindowManager方法：**
```
public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
        return new WindowManagerImpl(mContext, parentWindow);
    }
```
**该构造方法不同与在getSystemService中获得的对象，getSystemService获得的对象创建时调用的是一个context参数的方法：**
```
public WindowManagerImpl createPresentationWindowManager(Context displayContext) {
        return new WindowManagerImpl(displayContext, mParentWindow);
    }
```
**通过传入对应的window就可以获得关联的WindowManagerImpl对象**

##### 3、WindowManagerImpl类、该类是暴露给应用层的WindowManager实现类、该类的大致代码如下：
```
public final class WindowManagerImpl implements WindowManager {
    private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();
    private final Context mContext;
    private final Window mParentWindow;

    private IBinder mDefaultToken;

    public WindowManagerImpl(Context context) {
        this(context, null);
    }

    private WindowManagerImpl(Context context, Window parentWindow) {
        mContext = context;
        mParentWindow = parentWindow;
    }

    public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
        return new WindowManagerImpl(mContext, parentWindow);
    }

    @Override
    public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
        applyDefaultToken(params);
        mGlobal.addView(view, params, mContext.getDisplay(), mParentWindow);
    }

    @Override
    public void removeView(View view) {
        mGlobal.removeView(view, false);
    }

    ...
}

```
**可以看出其中调用窗口操作的具体类是WindowManagerGlobal**
##### 4、WindowManagerGlobal类：
**addView方法：**
```
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
    //省略判空代码
    ...
    
        ViewRootImpl root;
        View panelParentView = null;

    // 代码省略 
     ...
    
    //创建handler对象ViewRootImpl
            root = new ViewRootImpl(view.getContext(), display);

            view.setLayoutParams(wparams);

            //保存窗口参数
            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);

            // do this last because it fires off messages to start doing things
            try {
                //添加窗口并显示
                root.setView(view, wparams, panelParentView);
            } catch (RuntimeException e) {
                // BadTokenException or InvalidDisplayException, clean up.
                if (index >= 0) {
                    removeViewLocked(index, true);
                }
                throw e;
            }
        }
    }
```
**最终调用了ViewRootImpl的setView方法，在此方法前，先看下ViewRootImpl的构造函数：**
```
 public ViewRootImpl(Context context, Display display) {
        mContext = context;
        //在构造函数中通过WindowManagerGlobal.getWindowSession()获得进程通信接口
        mWindowSession = WindowManagerGlobal.getWindowSession();
        //保存当前创造ViewRootImpl的线程，只有在该线程中才能修改UI界面是因为所有窗口刷新都由该类与WMS进程交互，所以在这里保存了创建的进程、在修改UI线程操作时会检测该值，但如果在ViewRootImpl还没创建时就在其他线程调用了刷新方法，会绕过该判断
        mThread = Thread.currentThread();
        ...
    }
```
```
    public static IWindowSession getWindowSession() {
        synchronized (WindowManagerGlobal.class) {
            if (sWindowSession == null) {
                try {
                    InputMethodManager imm = InputMethodManager.getInstance();
                    //获得getWindowManagerService进程的AIDL接口
                    IWindowManager windowManager = getWindowManagerService();
                    sWindowSession = windowManager.openSession(
                            new IWindowSessionCallback.Stub() {
                                @Override
                                public void onAnimatorScaleChanged(float scale) {
                                    ValueAnimator.setDurationScale(scale);
                                }
                            },
                            imm.getClient(), imm.getInputContext());
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return sWindowSession;
        }
    }

```
**AIDL获得跨进程通信接口**
```
public static IWindowManager getWindowManagerService() {
        synchronized (WindowManagerGlobal.class) {
            if (sWindowManagerService == null) {
                sWindowManagerService = IWindowManager.Stub.asInterface(
                        ServiceManager.getService("window"));
                try {
                    if (sWindowManagerService != null) {
                        ValueAnimator.setDurationScale(
                                sWindowManagerService.getCurrentAnimatorScale());
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return sWindowManagerService;
        }
    }
```

**在构造函数中已经获得wms的跨进程aidl接口，最终调用的ViewRootImpl的setView方法、在其中调用远程接口添加窗口：**
```
    public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
        synchronized (this) {
            ...
                try {
                    mOrigWindowType = mWindowAttributes.type;
                    mAttachInfo.mRecomputeGlobalAttributes = true;
                    collectViewAttributes();
                    //调用跨进程通信接口方法-->即调用native层WMS方法
                    res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                            getHostVisibility(), mDisplay.getDisplayId(), mWinFrame,
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                            mAttachInfo.mOutsets, mAttachInfo.mDisplayCutout, mInputChannel);
                } catch (RemoteException e) {
                  //重置属性值
                  ...
                } finally {
                    if (restore) {
                        attrs.restore();
                    }
                }

                ...
                //设置当前View的mParent
                view.assignParent(this);
                ...
        }
    }
```
**其中最主要的就是：
1、 requestLayout()
2、mWindowSession.addToDisplay()**

**requestLayout函数：**
```
    @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            //request中会检测是否是之前构造函数检测的线程
            checkThread();
            mLayoutRequested = true;
            //请求重绘
            scheduleTraversals();
        }
    }
```
```
void scheduleTraversals() {
        //检测时间间隔
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            //Handler的同步屏障。它的作用是可以拦截Looper对同步消息的获取和分发，加入同步屏障之后，Looper只会获取和处理异步消息，如果没有异步消息那么就会进入阻塞状态。设置为异步消息后对View绘制渲染的处理操作可以优先处理。
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            //在Android4.1之后增加了Choreographer机制，用于同Vsync机制配合
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }
```
**其中有一个关键的地方：
mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
该方法最终会调用handler发送一个MSG_DO_SCHEDULE_VSYNC消息，导致doTraversal()的调用，即触发视图树的重绘。**
Choreographer该类用于管理绘制的同步时机。具体可以查看
>https://www.jianshu.com/p/bab0b454e39e


#### 总结：WindowManager首先会调用WindowManagerImpl这个桥接类，在其中实际调用的是WindowManagerGlobal类，该类中会有四个集合分别保存所有Windows对应的View,所有对应的ViewRootImpl，所有Windows的布局参数，和正在删除的View对象。其中主要的操作都需要通过ViewRootImpl中调用WindowSession这个Bind接口和系统的WindowManager做交互，并调用requestLayout刷新布局。

