---
layout:     post
title:      "Transform使用"
subtitle:   "Transform使用"
date:       2020-07-19 15:40:00
author:     "hzy"
//header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
    - AOP
---


## Transform使用开发

 - build.gradle.kts导入相关依赖

   ```kotlin
   plugins {
       `kotlin-dsl`
   }
   repositories {
       jcenter()
       google()
   }
   
   dependencies{
       //自定义插件导入jar 自定义Plugin需要
       gradleApi()
       //自定义Transfrom工具包 自定义Transform需要
       implementation("com.android.tools.build:gradle:3.6.1")
   }
   ```

    - 自定义Transform类 

   ```kotlin
   import com.android.build.api.transform.*
   import org.objectweb.asm.ClassReader
   import org.objectweb.asm.ClassWriter
   import java.io.File
   import java.io.FileOutputStream
   
   
   /**
    * User: hzy
    * Date: 2020/7/19
    * Time: 3:41 PM
    * Description: 自定义注入流程
    */
   class ClickTransform : Transform() {
       //Transform的名称
       override fun getName(): String {
           return this.javaClass.name
       }
   
       //处理文件类型--> class文件
       override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
           return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
       }
   
       //是否增量更新
       // 返回true的话表示支持，这个时候可以根据 com.android.build.api.transform.TransformInput 来获得更改、移除或者添加的文件目录或者jar包，参考CustomClassTransform
       override fun isIncremental(): Boolean {
           return false
       }
   
       //处理输入文件的范围，范围越小速度越快
       override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
           return mutableSetOf(QualifiedContent.Scope.PROJECT)
       }
   
       //处理过程
       override fun transform(transformInvocation: TransformInvocation) {
           //获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
           val outputProvider = transformInvocation?.outputProvider
           if (!isIncremental) outputProvider.deleteAll() //全量更新时删除所有输出代码
           //遍历输入的文件
           transformInvocation.inputs?.forEach { inputs ->
               inputs.directoryInputs.forEach { dir ->
                   traverseDir(dir, outputProvider)
               }
               //处理jar文件
               inputs.jarInputs.forEach { jar ->
                   traverseJar(jar, outputProvider)
               }
   
           }
   
       }
   
   
       private fun traverseJar(jarInput: JarInput, outputProvider: TransformOutputProvider) {
           //传递给下一个Transform处理  这里不处理jar包所以直接输出到输出目录给下一个Transform使用
           val outputJar = outputProvider.getContentLocation(
                   jarInput.name,
                   jarInput.contentTypes,
                   jarInput.scopes,
                   Format.JAR)
           //复制修改文件
           outputJar?.let { jarInput.file.copyRecursively(File(it.path), true) }
   
       }
   
       private fun traverseDir(dirInput: DirectoryInput, outputProvider: TransformOutputProvider?) {
           if (dirInput.file.isDirectory) {
               dirInput.file.walk().forEach {
                   if (it.isFile) transformFile(it, dirInput, outputProvider)
               }
           } else {
               transformFile(dirInput.file, dirInput, outputProvider)
           }
           //传递给下一个Transform处理
           val dest = outputProvider?.getContentLocation(
                   dirInput.name,
                   dirInput.contentTypes,
                   dirInput.scopes,
                   Format.DIRECTORY)
           //复制修改文件目录到输出目录下
           dest?.let { dirInput.file.copyRecursively(File(it.path), true) }
   
       }
   
       /***
        *
        */
       private fun transformFile(file: File, dirInput: DirectoryInput, outputProvider: TransformOutputProvider?) {
           //只拦截具体类名 MainAdapter  通过文件类型判断是否需要修改，这里可以动态配置
           if (file.name != "MainAdapter.class") return
           println("ClickTransform transformFile:${file.path}")
           val reader = ClassReader(file.readBytes())
           val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
         	//传入自定义的ClassVisitor到拦截器链中
           val visitor = ClickVisitor(writer)
           reader.accept(visitor, ClassReader.EXPAND_FRAMES)
           //原文件修改完之后需要，覆盖原来的文件
           val code = writer.toByteArray()
           val outputStream = FileOutputStream(file.path)
   
           outputStream.use {
               it.write(code)
           }
       }
   
   
   }
   ```

- 自定义Plugin类，添加

  ```kotlin
  package com.hzy.buildsrc
  
  import org.gradle.api.Plugin
  import org.gradle.api.Project
  
  /**
   * User: hzy
   * Date: 2020/7/15
   * Time: 12:23 PM
   * Description: 自定义Plugin
   */
  class ConsumePlugin : Plugin<Project> {
      override fun apply(pro: Project) {
          //注入转化
          val appExtension = pro.extensions.getByType(AppExtension::class.java)
          appExtension.registerTransform(ClickTransform())
      }
  }
  ```
  
- 根据处理流程处理输入文件

  ![输出处理流程](https://hzy-joel.github.io/img/post/transfrom.jpg)
  
- 自定义的C lassVisitor类：这个类用于访问类的信息，如果要处理方法就在visitMethod中包裹返回一个自定义的MethodVisitor对象在MethodVisitor中进行插桩，相应的如果需要针对类的注解解析可以在visitAnnotation中返回自定义的AnnotationVisitor对象

  ```kotlin
  package com.hzy.buildsrc.ClickTrack
  
  import org.objectweb.asm.ClassVisitor
  import org.objectweb.asm.MethodVisitor
  import org.objectweb.asm.Opcodes.ASM7
  
  
  /**
   * User: hzy
   * Date: 2020/7/19
   * Time: 8:05 PM
   * Description: ASM 遍历类
   */
  
  class ClickVisitor(private val visitor: ClassVisitor) : ClassVisitor(ASM7, visitor) {
      private var className: String? = null
      override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
          super.visit(version, access, name, signature, superName, interfaces)
          className = name
      }
  
      override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
          val visitor = super.visitMethod(access, name, descriptor, signature, exceptions)
          return ClickMethodVisitor(visitor, access, name, descriptor, className)
  
      }
      override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
          return super.visitAnnotation(descriptor, visible)
      }
  
  }
  ```

- 在自定义的MethodVisitor对象中进行插桩：在方法访问中，首先根据visitAnnotation访问注解判断该方法是否需要插桩，然后在真正进入方法的onMethodEnter中进行字节码插桩，插桩的代码可以通过ASM插件对比插桩后的java查看(kotlin不支持)

  ```kotlin
  package com.hzy.buildsrc.ClickTrack
  
  import org.objectweb.asm.AnnotationVisitor
  import org.objectweb.asm.Label
  import org.objectweb.asm.MethodVisitor
  import org.objectweb.asm.Opcodes
  import org.objectweb.asm.commons.AdviceAdapter
  
  /**
   * User: hzy
   * Date: 2020/7/19
   * Time: 8:42 PM
   * Description: 在方法前后插入代码
   */
  private val TAG = "ClickAnnotationAdapter:  "
  
  class ClickMethodVisitor(cv: MethodVisitor?, access: Int = 0, name: String?, desc: String?, private val className: String? = null) : AdviceAdapter(ASM7, cv, access, name, desc) {
  
      private var intercept = false
      override fun onMethodEnter() {
          super.onMethodEnter()
          if (!intercept) return
          //插桩方法
          mv?.let {
              //插桩显示Toast  该方法必须有context和text参数
              val l0 = Label()
              mv.visitLabel(l0)
              mv.visitLineNumber(16, l0)
              mv.visitVarInsn(Opcodes.ALOAD, 1)
              mv.visitVarInsn(Opcodes.ALOAD, 2)
              mv.visitInsn(Opcodes.ICONST_0)
              mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/widget/Toast", "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;", false)
              mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/widget/Toast", "show", "()V", false)
              val l1 = Label()
              mv.visitLabel(l1)
              mv.visitLineNumber(17, l1)
              mv.visitInsn(Opcodes.RETURN)
              val l2 = Label()
              mv.visitLabel(l2)
              mv.visitLocalVariable("this", "Lcom/hzy/cnn/CustomView/Utils/ASMCode;", null, l0, l2, 0)
              mv.visitLocalVariable("context", "Landroid/content/Context;", null, l0, l2, 1)
              mv.visitLocalVariable("text", "Ljava/lang/String;", null, l0, l2, 2)
              mv.visitMaxs(3, 3)
          }
  //        AsmLog.e(mv, "Click", "method:$name")
      }
  
      override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
          //访问注解判断是否要拦截方法 注解需要自定义并且生效范围要为Runtime
          intercept = descriptor?.contains("Lcom/hzy/aopdemo/Track/ClickTrack") == true
          println(TAG + "visitAnnotation:${descriptor} --> $intercept")
          return super.visitAnnotation(descriptor, visible)
      }
  
      override fun onMethodExit(opcode: Int) {
          super.onMethodExit(opcode)
      }
  
  }
  ```

  