package com.gmlive.soulmatch.repository.user.glue

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.*
import okhttp3.internal.wait
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * User: hzy
 * Date: 2020/6/19
 * Time: 2:11 PM
 * Description: 协程转换异步同步操作
 */
class Scope {
}


fun main(args: Array<String>) {
    runBlocking {
        print(getData3().await())
    }
}

// 同步转换 回调同步获取结果

suspend fun getData(): Int {
    return suspendCoroutine { continuation ->
        getInternetData(object : CallBack<Int> {
            override fun success(data: Int) {
                continuation.resume(data)
            }

            override fun error() {
                continuation.resumeWithException(Exception("error getData"))
            }
        })
    }
}

fun getData3(): Deferred<Int> {
    val deferred = CompletableDeferred<Int>()
    getInternetData(object : CallBack<Int>{
        override fun success(data: Int) {
            deferred.complete(data)
        }
        override fun error() {
            deferred.completeExceptionally(Exception("error"))
        }
    })
    return deferred
}


//同步转换 接口回调为livedata
fun getData2(): LiveData<Int> {
    return liveData(Dispatchers.IO) {
        emit(getData())
    }
}


fun getInternetData(callBack: CallBack<Int>){
    GlobalScope.launch {
        delay(1000)
        callBack.success(12)
    }
}
interface CallBack<T>{
    fun success(data:T)
    fun error()
}