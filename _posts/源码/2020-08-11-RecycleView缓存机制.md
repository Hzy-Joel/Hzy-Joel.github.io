## RecycleView缓存机制



在RecycleView中,由mAttachedScrap保存正在屏幕中的view缓存，由mChangedScrap保存即将动画变化的view，由mCachedViews保存被detach的View，RecyclerPool保存被mCachedViews中无法再存放的被remove的view。



#### 第一种情况复用，整体使用

