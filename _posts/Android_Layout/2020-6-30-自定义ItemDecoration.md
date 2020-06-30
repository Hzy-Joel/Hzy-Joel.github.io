---
layout:     post
title:      "自定义ItemDecoration"
subtitle:   "自定义ItemDecoration"
date:       2020-06-28 19:10:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - Android
    - 布局
    - RecycleView
---

## RecycleView
### 自定义ItemDecoration实现间距：
- 1.继承RecyclerView.ItemDecoration重写其getItemOffsets方法：
```
override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) 
```
其中第一个参数outRect为Decoration间距的距离4个方向的距离，在RecycleView布局时会加载itemView的padding上，可以通过以下代码设置距离
```
outRect.set(left,top,right,bottom)
```
- 2.针对不同布局管理器兼容间距
完整代码：
```
class SpanItemDecoration(val left: Int = 0, val right: Int = 0, val top: Int = 0, val bottom: Int = 0) : RecyclerView.ItemDecoration() {
        //实现边距
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            when (parent.layoutManager) {
                is GridLayoutManager -> getGridLayoutItemOffsets(outRect, view, parent, state, (parent.layoutManager as GridLayoutManager).spanCount)
                is StaggeredGridLayoutManager -> getStaggeredGridHorizontalItemOffsets(outRect, view, parent, state, (parent.layoutManager as StaggeredGridLayoutManager).spanCount, (parent.layoutManager as StaggeredGridLayoutManager).orientation)
                is LinearLayoutManager -> getLinearLayoutItemOffsets(outRect, view, parent, state, (parent.layoutManager as LinearLayoutManager).orientation)
            }
        }

        /***
         * 针对LinearLayoutManager的边距
         */
        private fun getLinearLayoutItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State, orientation: Int) {
            val itemCount = parent.adapter?.itemCount ?: 0
            val currentCount = parent.getChildAdapterPosition(view)
            when (orientation) {
                LinearLayoutManager.HORIZONTAL -> outRect.set(left.dp, top.dp, if (itemCount - 1 == currentCount) right.dp else 0, bottom.dp)
                LinearLayoutManager.VERTICAL -> outRect.set(left.dp, top.dp, right.dp, if (itemCount - 1 == currentCount) bottom.dp else 0)
            }
        }

        /***
         * 针对GridLayoutManager和StaggeredGridLayoutManager的VERTICAL模式的设置边距离
         */
        private fun getGridLayoutItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State, spanCount: Int) {
            val itemCount = parent.adapter?.itemCount ?: 0
            val currentCount = parent.getChildAdapterPosition(view)
            //每一行的最后一个View才需要添加right
            val lastOfLine = currentCount % spanCount == spanCount - 1 || currentCount == itemCount - 1
            outRect.set(left.dp, top.dp, if (lastOfLine) right.dp else 0, if (lastOfLine) bottom.dp else 0)
        }

        /***
         * 针对StaggeredGridLayoutManager的设置边距离
         */
        private fun getStaggeredGridHorizontalItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State, spanCount: Int, orientation: Int) {
            val params = view.layoutParams as StaggeredGridLayoutManager.LayoutParams
            val spanIndex = params.spanIndex StaggeredGridLayoutManager.LayoutParams
            val spanIndex = params.spanIndex
            val lastOfLine = spanIndex == spanCount - 1
            when (orientation) {
                RecyclerView.HORIZONTAL -> {
                    outRect.set(left.dp, top.dp, 0, if (lastOfLine) bottom.dp else 0)
                }
                RecyclerView.VERTICAL -> {
                    outRect.set(left.dp, top.dp, if (lastOfLine) right.dp else 0, 0)
                }
            }

        }


    }
```
其中可以采用**parent.adapter.itemCount**获得所有子View的数量，通过**parent.getChildAdapterPosition**获得该View在adapter中的位置(所有item)，通过**parent.getChildCount**获得所有可见的item的数量
针对瀑布流布局，需要使用StaggeredGridLayoutManager.LayoutParams中的spanIndex属性判断设置的第几列的边距。


### 自定义ItemDecoration实现分割线和悬浮头部动画：
#### 自定义ItemDecoration实现分割线：
- 1. 实现RecyclerView.ItemDecoration接口，重写getItemOffsets方法为分割线留出空隙。

```
override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            if (parent.layoutManager !is LinearLayoutManager) return
            val position = parent.getChildAdapterPosition(view)
            //如果是最后一个item就不需要画分割线
            val needDivider = position != parent.adapter?.itemCount ?: 0 - 1
            outRect.bottom = if (needDivider) dividerHeight.dp else 0
        }
```
- 2.重写onDraw方法对当前布局内的所有View绘制分割线
```
 override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val left: Int = parent.paddingLeft
            val right: Int = parent.width - parent.paddingRight
            //获取屏幕可见的view数量
            val childCount: Int = parent.childCount
            for (i in 0 until childCount) {
                //计算分割线起始位置
                val child: View = parent.getChildAt(i)
                val params = child
                        .layoutParams as RecyclerView.LayoutParams
                // divider的top 应该是 item的bottom 加上 marginBottom 再加上 Y方向上的位移  -->计算起始y坐标
                val top = child.bottom + params.bottomMargin +
                        child.translationY.roundToInt()
                // divider的bottom就是top加上divider的高度了
                val bottom = (top + dividerHeight.dp)         //计算结束y坐标
                c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint) //画笔自定义
            }
            super.onDraw(c, parent, state)

        }
```

#### 实现悬浮头部：
- 1. 实现悬浮原理：由于悬停头部需要在ItemView之上显示，所以绘制要重写onDrawOver来绘制。简单的图形可以使用Canvas绘制，复杂的布局需要使用自定义的View再将其layout后再使用Canvas绘制出来。由于如果使用onDrawOver，绘制的view会覆盖在itemView之上，这个时候需要跟随RecycleView滚动的话，可以采用Canvas的translate方法将画布移动再绘制
```
        //仍然需要在getItemOffsets为其留出空间，否则会覆盖在ItemView之上
        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDrawOver(c, parent, state)
            //获得布局中第一个View的位置
            val child: View = parent.getChildAt(0)
            if (parent.getChildAdapterPosition(child) != 0) {
                //获取是否是adapter第一个View -->这里可以控制只在第一个Vie时显示或者分组的时候显示
                // 如果第一个item已经被回收,没有显示在recycler view上,则不需要draw header
                return
            }
            val params = child
                    .layoutParams as RecyclerView.LayoutParams
            // divider的top 应该是 item的bottom 加上 marginBottom 再加上 Y方向上的位移
            val top = child.top + params.topMargin -
                    child.translationY.roundToInt()
            //移动画布达到绘制的效果随布局移动的效果 -->每次滚动都会调用重绘，从而每次的位置值可以通过itemView的顶部偏移算出画布的移动
            c.translate(0.toFloat(), top - dividerHeight.dp.toFloat())
            //模拟一个自定义的View
            val view = TextView(parent.context).apply {
                text = "1231412412412"
                height = dividerHeight.dp
            }
            //一定要先layout自定义的View
            view.layout(0, 0, 200.dp, dividerHeight.dp)
            view.draw(c)
            c.translate(0.toFloat(), -(top - dividerHeight.dp.toFloat()))
        }
```





