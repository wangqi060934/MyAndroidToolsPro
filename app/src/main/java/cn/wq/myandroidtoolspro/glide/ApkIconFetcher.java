package cn.wq.myandroidtoolspro.glide;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import cn.wq.myandroidtoolspro.BuildConfig;

public class ApkIconFetcher implements DataFetcher<Drawable> {
    private String packageName;
    private Context context;
    public final static String PREFIX = "icon:";

    public ApkIconFetcher(String packageName, Context context) {
        this.packageName = packageName;
        this.context = context;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Drawable> callback) {
        if (!packageName.startsWith(PREFIX)) {
            callback.onLoadFailed(new IllegalAccessException("not valid param,must start with 'icon:'"));
            return;
        }
        try {
            Drawable drawable = context.getPackageManager().getApplicationIcon(packageName.substring(PREFIX.length()));
            callback.onDataReady(drawable);
        } catch (PackageManager.NameNotFoundException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<Drawable> getDataClass() {
        return Drawable.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
