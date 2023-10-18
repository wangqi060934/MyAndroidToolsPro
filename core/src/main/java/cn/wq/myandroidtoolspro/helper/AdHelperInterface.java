package cn.wq.myandroidtoolspro.helper;

import android.content.Context;

import cn.wq.myandroidtoolspro.BaseActivity;

public interface AdHelperInterface {
    void initAd(Context context);
    void startLoadAd(BaseActivity act);
    void stopAd(Context context);
}
