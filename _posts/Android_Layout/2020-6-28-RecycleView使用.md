---
layout:     post
title:      "RecycleView使用"
subtitle:   "RecycleView使用"
date:       2020-06-28 19:10:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - Android
    - 布局
---

## RecycleView
#### ItemTouchHelper实现对RecycleView交互
- 1.实现ItemTouchHelper.Callback接口:可以使用ItemTouchHelper.SimpleCallback实现，构造函数传入需要支持的滑动方向，dragDirs代表上下，swipeDirs代表左右，不需要支持则传0，如下代表监听上下左右滑动
```
class TouchCallBack : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) 
```
- 2.重写Callback接口的相关方法：
```
    class TouchCallBack : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        /***
         *   上下滑动后触发 也可以用helper.startDrag()指定触发
         */
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            //拖动后改变位置交换
            Collections.swap(adapter.getData(), viewHolder.adapterPosition, target.adapterPosition)
            adapter.notifyItemRangeChanged(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }
    
        /***
         * 左右滑动后触发
         */
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            //滑动删除
            adapter.getData().removeAt(viewHolder.adapterPosition)
            adapter.notifyItemRemoved(viewHolder.adapterPosition)
        }
    
        /***
         * 长按启动拖动 ，如果使用helper.startDrag()用别的方式触发拖动这里要返回false
         */
        override fun isLongPressDragEnabled(): Boolean = true
    
    
        /***
         * 在被拖动时或左右滑动开始时会触发 --可以做例如拖动或滑动时修改背景色
         */
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
        }
    
        /***
         * 在被拖动左右滑动结束时会触发 --可以在滑动结束之后复位
         */
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
        }
    
        /***
         * 被拖拽或滑动时会触发-->可以拿到位移 可以用于做一些item切换时的动画效果，例如卡片滑动
         */
        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    
        /***
         * 在绘制之后触发，可以做到item覆盖
         */
        override fun onChildDrawOver(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
```

