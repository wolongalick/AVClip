package com.alick.commonlibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/11 22:52
 */
abstract class BaseActivity<Binding : ViewBinding> : AppCompatActivity() {

    lateinit var viewBinding: Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createView())

        initListener()
        initData()
    }

    private fun createView(): View {
        //通过泛型、反射的方式获取mViewBinding对象
        val genericSuperclass = this.javaClass.genericSuperclass
        val parameterizedType = genericSuperclass as ParameterizedType
        val type = parameterizedType.actualTypeArguments[0]
        val entityClass: Class<Binding> = type as Class<Binding>
        try {
            val method = entityClass.getMethod("inflate", LayoutInflater::class.java)
            viewBinding = method.invoke(null, LayoutInflater.from(this)) as Binding
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return viewBinding.root
    }

    /**
     * 初始化监听事件
     */
    abstract fun initListener()

    /**
     * 初始化数据
     */
    abstract fun initData()
}