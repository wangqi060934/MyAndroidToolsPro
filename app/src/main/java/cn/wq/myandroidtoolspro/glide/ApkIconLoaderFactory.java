package cn.wq.myandroidtoolspro.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

public class ApkIconLoaderFactory implements ModelLoaderFactory<String, Drawable> {
    private Context context;

    ApkIconLoaderFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<String,Drawable> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new ApkIconModelLoader(context);
    }

    @Override
    public void teardown() {

    }
}
