package com.alick.utilslibrary;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;


/**
 * Created by cxw on 2016/4/15.
 */
public class EditTextUtils {
    /**
     * 将光标移动到末尾
     */
    public static void moveCursorToLast(TextView textView) {
        if (textView instanceof EditText) {
            ((EditText) textView).setSelection(textView.getText().length());
        }
    }

    /**
     * 限制文本内容长度,超出时,光标移动到末尾
     *
     * @param textView
     */
    public static void limitCount(final TextView textView, final int limit) {
        textView.addTextChangedListener(new SimpleTextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                String str = textView.getText().toString();
                if (str.length() > limit) {
                    str = str.substring(0, limit);

                    // 如果内容是表情,还需要用SmileUtils类转换一下

                    textView.setText(str);

                    //如果是输入框,则将光标移动到末尾
                    moveCursorToLast(textView);
                }
            }
        });
    }

    public static void setTextCompletedListener(final IOnTextComplatedListener iOnTextComplatedListener, final TextView... textViews) {
        for (TextView textView : textViews) {
            textView.addTextChangedListener(new SimpleTextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    for (TextView textView : textViews) {
                        if (TextUtils.isEmpty(textView.getText().toString().trim())) {
                            iOnTextComplatedListener.onTextChanged(false);
                            return;
                        }
                    }
                    iOnTextComplatedListener.onTextChanged(true);
                }
            });
        }
    }

    public interface IOnTextComplatedListener {
        void onTextChanged(boolean isCompleted);
    }


    /**
     * 复制到剪贴板
     *
     * @param context
     * @param text
     */
    public static void copy2Clipboard(Context context, String text) {
        if (context == null || text == null) {
            return;
        }

        ClipboardManager clipboardManager = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, text);
        assert clipboardManager != null;
        clipboardManager.setPrimaryClip(clipData);
    }

    /**
     * 调用删除键,可用于一键删除emoji字符
     * @param editText
     */
    public static void invokeDeleteKey(EditText editText) {
        int      keyCode      = KeyEvent.KEYCODE_DEL;
        KeyEvent keyEventDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyEventUp   = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        editText.onKeyDown(keyCode, keyEventDown);
        editText.onKeyUp(keyCode, keyEventUp);
    }

}
