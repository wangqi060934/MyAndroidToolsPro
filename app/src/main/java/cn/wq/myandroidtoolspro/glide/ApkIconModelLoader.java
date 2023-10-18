package cn.wq.myandroidtoolspro.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

public class ApkIconModelLoader implements ModelLoader<String, Drawable> {
    private Context context;

    ApkIconModelLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<Drawable> buildLoadData(@NonNull String s, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(s), new ApkIconFetcher(s, context));
    }

    @Override
    public boolean handles(@NonNull String s) {
        return s.startsWith(ApkIconFetcher.PREFIX);
    }

}
