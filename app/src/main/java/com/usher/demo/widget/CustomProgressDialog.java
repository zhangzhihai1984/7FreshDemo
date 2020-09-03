package com.usher.demo.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import com.usher.demo.R;

public class CustomProgressDialog extends Dialog {

    private Context mContext;

    public CustomProgressDialog(final Context context) {
        super(context, R.style.customProgressDialog);
        this.mContext = context;

        this.setCanceledOnTouchOutside(false);
        this.setCancelable(false);

        setTouchAble(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        setContentView(R.layout.custom_progress_dialog_layout);
    }

    public void setTouchAble(boolean isTouchAble) {

        if (isTouchAble) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.dimAmount = 0.5f;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }
}
