package com.alick.utilslibrary

import android.os.Looper
import android.widget.Toast

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/13 19:00
 */
class T {
    companion object{
        fun show(msg:String){
            if(Looper.myLooper() == Looper.getMainLooper()){
                Toast.makeText(AppHolder.getApp(),msg,Toast.LENGTH_SHORT).show()
            }else{
                val handler=WeakHandler(Looper.getMainLooper())
                handler.post {
                    Toast.makeText(AppHolder.getApp(),msg,Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}