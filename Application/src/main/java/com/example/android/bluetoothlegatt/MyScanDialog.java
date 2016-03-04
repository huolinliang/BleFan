package com.example.android.bluetoothlegatt;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by leung on 2015/11/28.
 */
public class MyScanDialog extends ProgressDialog {
    private AnimationDrawable mAnimation;
    private ImageView mImageView;
    //private TextView mTextView;
    //private String loadingTip;
    private int resid;

    /**
     *
     * @param context 上下文对象
     */
    public MyScanDialog(Context context, int resid) {
        super(context);
        //this.loadingTip = content;
        this.resid = resid;
        //点击提示框外面是否取消提示框
        setCanceledOnTouchOutside(true);
        //点击返回键是否取消提示框
        setCancelable(true);
        setIndeterminate(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_dialog);

        //mTextView = (TextView) findViewById(R.id.scanTv);
        mImageView = (ImageView) findViewById(R.id.scanIv);

        mImageView.setBackgroundResource(resid);
        // 通过ImageView对象拿到背景显示的AnimationDrawable
        mAnimation = (AnimationDrawable) mImageView.getBackground();

        mImageView.post(new Runnable() {
            @Override
            public void run() {
                mAnimation.start();
            }
        });
        //mTextView.setText(loadingTip);
    }
}
