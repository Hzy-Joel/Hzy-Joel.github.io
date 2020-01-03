---
layout:     post
title:      "Materil Design 布局"
subtitle:   " Materil Design 布局使用"
date:       2020-01-2 12:00:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - Android
    - 布局
---

# Materil Design 布局：


### CoordinatorLayout：实现联动布局
- layout_behavior属性（该属性必须是AppBarLayout的直接子布局）：
    - @string/appbar_scrolling_view_behavior：当布局中存在滚动布局时需要设置该属性通知布局具有滚动布局





### AppBarLayout：实现子控件滚动联动布局
#### 布局属性：
-  layout_scrollFlags属性：  
   - scroll：设置可滚动，即跟随布局中可滚动的View一起滚动，该属性必须设置，否则其他属性无效 
   - enterAlways：向下滚动时该布局立刻出现，即快速返回功能
   - exitUntilCollapsed：设置了minHeight后  滚动只会折叠到最小高度不会全部消失
   - enterAlwaysCollapsed：设置了最小高度后，向下滑动会先滑动这个布局到最小高度再滑动其他控件，这个属性最好设置为scroll|enterAlways|enterAlwaysCollapsed，因为只有在其他滑动控件可向下滑动时再滑动才能看出效果是先滑动了这个布局再滚动其他布局。


### CollapsingToolbarLayout：实现折叠布局
#### 布局属性：
- layout_collapseMode：设置折叠模式属性
    - off：默认属性，布局正常显示， 没有折叠效果
    - pin：折叠后停留在顶部（初始不会跟随滚动控件移动，当到达底部触及CollapsingToolbarLayout布局底部后跟随滚动向上，最后的效果会停留在CollapsingToolbarLayout布局的底部）
    - parallax：视差折叠效果（根据滚动视图的滚动按照（layout_collapseParallaxMultiplier的属性值）滚动该控件）
- layout_collapseParallaxMultiplier：设置滚动视差因子，在layout_collapseMode属性设置为parallax后该设置生效，该值取值为0~1，对应是指布局占据该CollapsingToolbarLayout布局下半部分的比例）。也可以直观认为是该控件跟随滚动控件移动的速率转换，当为0 时跟随滚动一起滚动，为0.5时只滚动一半的速率。
- contentScrim=”?attr/colorPrimary”：设置属性之后折叠toolbar会显示主题的颜色  

  
  
    
      
      

### AppBarLayout自定义Behavior实现：  
- 实现CoordinatorLayout.Behavior<View>接口：其中的View泛型指定使用该BehaViro的控件类型，记得重写两个参数方法才能在布局中使用。
- 接口方法：
   -  boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency)，其中parent为根布局，child为使用该Behavior的控件，dependency为监听的控件，返回值为true表示监听dependency的变化，如果想指定监听RecycleView的可以这样写： 
        ``` 
        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {   
            return dependency instanceof RecyclerView;
        } 
        ```   
   - boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) ：在滚动控件dependency位置改变时调用，即dependency.getY()属性变化时，（该函数只会在dependency的位置变化时和初始化时调用，内部的滚动滚动会调用NestedScrolling接口的方法）



  












  









