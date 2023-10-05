package com.alick.utilslibrary

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @createTime 2023/3/13 2:35
 * @author 崔兴旺
 * @description
 */
class ThreadPoolManager private constructor(corePoolSize: Int, maximumPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, workQueue: BlockingQueue<Runnable>) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {

    companion object {
        private const val TAG = "ThreadPoolManager"

        private val executors: ThreadPoolManager = ThreadPoolManager(20, 64, 60L, TimeUnit.SECONDS, LinkedBlockingQueue())


        fun execute(runnable: Runnable) {
            executors.execute(runnable)
        }
    }

    override fun beforeExecute(t: Thread?, r: Runnable?) {
//        PenLogUtils.i(TAG, "线程池执行前,activeCount:${executors.activeCount},taskCount:${executors.taskCount},completedTaskCount:${executors.completedTaskCount},poolSize:${executors.poolSize},largestPoolSize:${executors.largestPoolSize}")
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
//        BLog.i(TAG, "线程池执行后,activeCount:${executors.activeCount},taskCount:${executors.taskCount},completedTaskCount:${executors.completedTaskCount},poolSize:${executors.poolSize},largestPoolSize:${executors.largestPoolSize}")
    }

}