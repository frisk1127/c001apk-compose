package net.mikaelzero.mojito.view.sketch;

import android.net.Uri;
import android.util.Log;
import android.view.View;

import net.mikaelzero.mojito.interfaces.ImageViewLoadFactory;
import net.mikaelzero.mojito.loader.ContentLoader;
import net.mikaelzero.mojito.view.sketch.core.Sketch;
import net.mikaelzero.mojito.view.sketch.core.SketchImageView;

import org.jetbrains.annotations.NotNull;

import java.io.File;


public class SketchImageLoadFactory implements ImageViewLoadFactory {
    @Override
    public void loadSillContent(@NotNull View view, @NotNull Uri uri) {
        if (view instanceof SketchImageView) {
            SketchImageView sketchView = (SketchImageView) view;
            String path = uri.getPath();
            long length = 0L;
            if (path != null) {
                File file = new File(path);
                if (file.isFile()) {
                    length = file.length();
                }
            }
            Log.d("MojitoLongPress", "loadSillContent uri=" + uri + " size=" + length);
            Sketch.with(view.getContext()).display(path, sketchView).loadingImage((context, imageView, displayOptions) -> {
                return sketchView.getDrawable(); // 解决缩略图切换到原图显示的时候会闪烁的问题
            }).commit();
            sketchView.post(() -> {
                if (sketchView.getZoomer() != null) {
                    Log.d("MojitoLongPress", "post reset zoomer");
                    sketchView.getZoomer().reset("postLoad");
                }
            });
        }
    }

    @Override
    public void loadContentFail(@NotNull View view, int drawableResId) {
        if (view instanceof SketchImageView) {
            ((SketchImageView) view).displayResourceImage(drawableResId);
        }
    }

    @NotNull
    @Override
    public ContentLoader newContentLoader() {
        return new SketchContentLoaderImpl();
    }
}
