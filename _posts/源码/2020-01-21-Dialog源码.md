---
layout:     post
title:      "Dialog源码分析"
subtitle:   " Dialog源码分析"
date:       2020-01-21 14:36:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - 源码
---






### Dialog源码分析
##### 1、Dialog的使用方法：
```
AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainDialogActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("我是一个普通Dialog");
normalDialog.show();
```
##### 2、其中调用了AlertDialog.Builder的show方法：
```
 public AlertDialog create() {
            // We can't use Dialog's 3-arg constructor with the createThemeContextWrapper param,
            // so we always have to re-set the theme
            final AlertDialog dialog = new AlertDialog(P.mContext, mTheme);
            P.apply(dialog.mAlert);
            dialog.setCancelable(P.mCancelable);
            if (P.mCancelable) {
                dialog.setCanceledOnTouchOutside(true);
            }
            dialog.setOnCancelListener(P.mOnCancelListener);
            dialog.setOnDismissListener(P.mOnDismissListener);
            if (P.mOnKeyListener != null) {
                dialog.setOnKeyListener(P.mOnKeyListener);
            }
            return dialog;
        }

        /**
         * Creates an {@link AlertDialog} with the arguments supplied to this
         * builder and immediately displays the dialog.
         * <p>
         * Calling this method is functionally identical to:
         * <pre>
         *     AlertDialog dialog = builder.create();
         *     dialog.show();
         * </pre>
         */
        public AlertDialog show() { 
            final AlertDialog dialog = create();
            //调用了Dialog的show方法显示
            dialog.show();
            return dialog;
        }
```
**在调用show方法时先使用create()方法将Builder的参数用于构造一个AlertDialog并调用show方法。在AlertDialog并没有覆盖show方法、因此调用的是父类Dialog的show方法。**
##### 3、Dialog.show()方法：
```
public void show() {
        if (mShowing) {
            if (mDecor != null) {
                if (mWindow.hasFeature(Window.FEATURE_ACTION_BAR)) {
                    mWindow.invalidatePanelMenu(Window.FEATURE_ACTION_BAR);
                }
                mDecor.setVisibility(View.VISIBLE);
            }
            return;
        }

        mCanceled = false;

        if (!mCreated) {
            //触发onCreate事件
            dispatchOnCreate(null);
        } else {
            // Fill the DecorView in on any configuration changes that
            // may have occured while it was removed from the WindowManager.
            final Configuration config = mContext.getResources().getConfiguration();
            mWindow.getDecorView().dispatchConfigurationChanged(config);
        }
        //调用OnStart方法
        onStart();
        mDecor = mWindow.getDecorView();

        if (mActionBar == null && mWindow.hasFeature(Window.FEATURE_ACTION_BAR)) {
            final ApplicationInfo info = mContext.getApplicationInfo();
            mWindow.setDefaultIcon(info.icon);
            mWindow.setDefaultLogo(info.logo);
            mActionBar = new WindowDecorActionBar(this);
        }

        WindowManager.LayoutParams l = mWindow.getAttributes();
        if ((l.softInputMode  & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) == 0) {
            WindowManager.LayoutParams nl = new WindowManager.LayoutParams();
            nl.copyFrom(l);
            nl.softInputMode |=
                    WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
            l = nl;
        }
        
        //添加View
        mWindowManager.addView(mDecor, l);
        mShowing = true;

        sendShowMessage();
    }
```
    
```  
    //调用onCreate事件
    void dispatchOnCreate(Bundle savedInstanceState) {
        if (!mCreated) {
            onCreate(savedInstanceState);
            mCreated = true;
        }
    }
  ```
  **该方法中流程为：**
  **1. 调用onCreate方法**
  **2. 调用onStart方法**
  **3. 调用mWindowManager.addView(mDecor, l)添加View**
  **4. 调用sendShowMessage**
  
  

* * *

#### 1、onCreate方法：
######   Dialog的onCreate方法为空实现，方法实现被AlertDialog的onCreate覆盖：
```
 @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //调用了AlertController类的installContent，该类是保存Dialog的构建信息类，Builer就是用来构建该类并用其来初始化Dialog的
        mAlert.installContent();
    }
```
#####  AlertController类的installContent方法：
```
public void installContent() {
        final int contentView = selectContentView();
        //设置内容布局
        mDialog.setContentView(contentView);
        //
        setupView();
    }
```
selectContentView方法根据主题选择返回不同的布局
```
 private int selectContentView() {
        if (mButtonPanelSideLayout == 0) {
            return mAlertDialogLayout;
        }
        if (mButtonPanelLayoutHint == AlertDialog.LAYOUT_HINT_SIDE) {
            return mButtonPanelSideLayout;
        }
        return mAlertDialogLayout;
    }
```
setupView方法设置布局中子视图的内容
```
private void setupView() {
        final View parentPanel = mWindow.findViewById(R.id.parentPanel);
        final View defaultTopPanel = parentPanel.findViewById(R.id.topPanel);
        final View defaultContentPanel = parentPanel.findViewById(R.id.contentPanel);
        final View defaultButtonPanel = parentPanel.findViewById(R.id.buttonPanel);

        // Install custom content before setting up the title or buttons so
        // that we can handle panel overrides.
        //用户可以自定义布局替换该区域位置就是在这里设置的
        final ViewGroup customPanel = (ViewGroup) parentPanel.findViewById(R.id.customPanel);
        setupCustomContent(customPanel);

        final View customTopPanel = customPanel.findViewById(R.id.topPanel);
        final View customContentPanel = customPanel.findViewById(R.id.contentPanel);
        final View customButtonPanel = customPanel.findViewById(R.id.buttonPanel);

        // Resolve the correct panels and remove the defaults, if needed.
        final ViewGroup topPanel = resolvePanel(customTopPanel, defaultTopPanel);
        final ViewGroup contentPanel = resolvePanel(customContentPanel, defaultContentPanel);
        final ViewGroup buttonPanel = resolvePanel(customButtonPanel, defaultButtonPanel);

        setupContent(contentPanel);
        setupButtons(buttonPanel);
        setupTitle(topPanel);

        final boolean hasCustomPanel = customPanel != null
                && customPanel.getVisibility() != View.GONE;
        final boolean hasTopPanel = topPanel != null
                && topPanel.getVisibility() != View.GONE;
        final boolean hasButtonPanel = buttonPanel != null
                && buttonPanel.getVisibility() != View.GONE;

        // Only display the text spacer if we don't have buttons.
        if (!hasButtonPanel) {
            if (contentPanel != null) {
                final View spacer = contentPanel.findViewById(R.id.textSpacerNoButtons);
                if (spacer != null) {
                    spacer.setVisibility(View.VISIBLE);
                }
            }
        }

        if (hasTopPanel) {
            // Only clip scrolling content to padding if we have a title.
            if (mScrollView != null) {
                mScrollView.setClipToPadding(true);
            }

            // Only show the divider if we have a title.
            View divider = null;
            if (mMessage != null || mListView != null) {
                divider = topPanel.findViewById(R.id.titleDividerNoCustom);
            }

            if (divider != null) {
                divider.setVisibility(View.VISIBLE);
            }
        } else {
            if (contentPanel != null) {
                final View spacer = contentPanel.findViewById(R.id.textSpacerNoTitle);
                if (spacer != null) {
                    spacer.setVisibility(View.VISIBLE);
                }
            }
        }

        if (mListView instanceof RecycleListView) {
            ((RecycleListView) mListView).setHasDecor(hasTopPanel, hasButtonPanel);
        }

        // Update scroll indicators as needed.
        if (!hasCustomPanel) {
            final View content = mListView != null ? mListView : mScrollView;
            if (content != null) {
                final int indicators = (hasTopPanel ? ViewCompat.SCROLL_INDICATOR_TOP : 0)
                        | (hasButtonPanel ? ViewCompat.SCROLL_INDICATOR_BOTTOM : 0);
                setScrollIndicators(contentPanel, content, indicators,
                        ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
            }
        }

        final ListView listView = mListView;
        if (listView != null && mAdapter != null) {
            listView.setAdapter(mAdapter);
            final int checkedItem = mCheckedItem;
            if (checkedItem > -1) {
                listView.setItemChecked(checkedItem, true);
                listView.setSelection(checkedItem);
            }
        }
    }
```
在之上的设置布局所获得布局View都是在AlertController的构造函数中获得
```
 public AlertController(Context context, AppCompatDialog di, Window window) {
        mContext = context;
        mDialog = di;
        mWindow = window;
        mHandler = new ButtonHandler(di);

        final TypedArray a = context.obtainStyledAttributes(null, R.styleable.AlertDialog,
                R.attr.alertDialogStyle, 0);

        mAlertDialogLayout = a.getResourceId(R.styleable.AlertDialog_android_layout, 0);
        mButtonPanelSideLayout = a.getResourceId(R.styleable.AlertDialog_buttonPanelSideLayout, 0);

        mListLayout = a.getResourceId(R.styleable.AlertDialog_listLayout, 0);
        mMultiChoiceItemLayout = a.getResourceId(R.styleable.AlertDialog_multiChoiceItemLayout, 0);
        mSingleChoiceItemLayout = a
                .getResourceId(R.styleable.AlertDialog_singleChoiceItemLayout, 0);
        mListItemLayout = a.getResourceId(R.styleable.AlertDialog_listItemLayout, 0);
        mShowTitle = a.getBoolean(R.styleable.AlertDialog_showTitle, true);
        mButtonIconDimen = a.getDimensionPixelSize(R.styleable.AlertDialog_buttonIconDimen, 0);

        a.recycle();

        /* We use a custom title so never request a window title */
        di.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }
```
#### 2、调用onStart方法：
```
protected void onStart() {
        if (mActionBar != null) mActionBar.setShowHideAnimationEnabled(true);
    }
```

#### 3、调用mWindowManager.addView(mDecor, l)添加View：
**方法涉及到WMS的管理，这里暂时只记作用，将View添加到Window窗口中**

#### 4、sendShowMessage
```
private void sendShowMessage() {
        if (mShowMessage != null) {
            // Obtain a new message so this dialog can be re-used
            Message.obtain(mShowMessage).sendToTarget();
        }
    }
```

  