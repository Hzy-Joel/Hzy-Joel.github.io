---
layout:     post
title:      "LeakCanary原理"
subtitle:   "LeakCanary原理"
date:       2020-12-10 23:18:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:   
      - 三方库
      - 内存优化
---



##  如何确定一个对象是否被回收
    - 1、 在对象的finalize方法被调用时，确定是被回收
    - 2、 使用双参数构造函数WeakReference(T referent, ReferenceQueue<? super T> q) 创建弱、软、虚引用持有对象、在传入的第二个参数中
    如果该引用指向的对象已经被回收、那么会使用第二个参数ReferenceQueue保存该持有对象，如果没有那说明没被回收。


##  其2.0版本如何不需要手动install：
    - 注册使用ContentProvider，在其onCreate中进行注册。



    


