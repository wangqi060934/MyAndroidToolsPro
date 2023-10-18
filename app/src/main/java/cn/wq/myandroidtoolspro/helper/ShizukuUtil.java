package cn.wq.myandroidtoolspro.helper;

import com.tencent.mars.xlog.Log;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import moe.shizuku.api.ShizukuClient;

public class ShizukuUtil {
    private static final String TAG = "ShizukuUtil";
    public static  Disposable getIsAuthorised(final Consumer<Boolean> success, final Consumer<Throwable> failed){
        return Observable.create(new ObservableOnSubscribe<Boolean>(){
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                emitter.onNext(ShizukuClient.getState().isAuthorized());
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean && success != null) {
                            success.accept(true);
                        } else if (!aBoolean && failed != null) {
                            failed.accept(null);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG,"ShizukuClient.getState().isAuthorized() error",throwable);
                        if (failed != null) {
                            failed.accept(throwable);
                        }
                    }
                });
    }
}
