package com.alick.avsdk.util;

import com.alick.utilslibrary.BLog;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author 崔兴旺
 * @description
 * @date 2022/4/1 23:00
 */
public class AudioMix {
    private static float normalizeVolume(int volume) {
        return volume / 100f * 1;
    }

    //     vol1  vol2  0-100  0静音  120
    public static void mixPcm(String pcm1Path, String pcm2Path, String toPath
            , int volume1, int volume2) throws IOException {
        float vol1 = normalizeVolume(volume1);
        float vol2 = normalizeVolume(volume2);
//一次读取多一点 2k
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];
//        待输出数据
        byte[] buffer3 = new byte[2048];

        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);

//输出PCM 的
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);
        short            temp2, temp1;//   两个short变量相加 会大于short   声音
        int              temp;
        boolean          end1             = false, end2 = false;
        while (!end1 || !end2) {
            //
            int readSize1 = is1.read(buffer1);
            end1 = (readSize1 == -1);

            if (!end1) {
                //音乐的pcm数据  写入到 buffer3
                BLog.Companion.i("readSize1:" + readSize1, "cxw");
                System.arraycopy(buffer1, 0, buffer3, 0, readSize1);

                end2 = (is2.read(buffer2) == -1);
                for (int i = 0; i < buffer2.length; i += 2) {
                    if (i < readSize1) {
//                    或运算
                        temp1 = (short) ((buffer1[i] & 0xff) | ((buffer1[i + 1] & 0xff) << 8));
                        temp2 = (short) ((buffer2[i] & 0xff) | ((buffer2[i + 1] & 0xff) << 8));
                        temp = (int) (temp1 * vol1 + temp2 * vol2);//音乐和 视频声音 各占一半
                        if (temp > Short.MAX_VALUE) {
                            temp = Short.MAX_VALUE;
                        } else if (temp < Short.MIN_VALUE) {
                            temp = Short.MIN_VALUE;
                        }
                        buffer3[i] = (byte) (temp & 0xFF);
                        buffer3[i + 1] = (byte) ((temp >>> 8) & 0xFF);
                    } else {
                        buffer3[i] = buffer2[i];
                    }
                }
            } else {
                int readSize2 = is2.read(buffer2);
                end2 = (readSize2 == -1);
                if(!end2){
                    System.arraycopy(buffer2, 0, buffer3, 0, readSize2);
                }
            }
            fileOutputStream.write(buffer3);
        }
        is1.close();
        is2.close();
        fileOutputStream.close();
    }
}