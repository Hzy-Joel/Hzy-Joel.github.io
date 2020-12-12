---
layout:     post
title:      "BlockCanary原理"
subtitle:   "界面耗时检测"
date:       2020-12-12 23:27:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:   
      - 三方库
      - 渲染优化
---



## BlockCanary原理
    - 如何检测耗时：
        1、通过使用Looper.getMainLooper().setMessageLogging方法捕捉Printer在Looper的loop方法中对每个message的分派处理前后的打印时机记录每个message的处理耗时，通过打印的字符串区分开始前后。
        2、还可以通过编舞者Choreographer的每一帧回调方法记录耗时。
    - 如何找到方法耗时点：
        1、通过Thread.currentThread.getStackTrace方法将每个调用栈打印出来。


## 如何辅助找到耗时点（工具）:
    - 1、通过Debug.startMethodTracing方法和Debug.stopMethodTracing方法生成track文件，查看绘制耗时点（但是该方法有误差，对性能有影响）
    - 2、通过Trace.beginSection方法和Trace.endSection方法打开监控，再通过sdk工具包下platform-tools/systrace的systrace.py脚本生成html文件。
         或者通过手机的设置中的System Tracing方法生成文件再到官网解析（或者systrace --from-file trace-file-name.ctrace）。





#### 小知识点：
    - 优化日志输出：通过Timber.plant方法对日志进行格式化、还可以直接点日志跳转对应代码处。




    


