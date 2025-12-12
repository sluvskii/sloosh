package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Similar to {@link PreDrawBlurController} but applies an alpha mask Drawable on top of the blurred
 * snapshot so the blur shows only where mask alpha is set. Designed to blur underlying content
 * (backdrop) with a gradient mask.
 */
public final class GradientPreDrawBlurController implements BlurController {

    @ColorInt
    public static final int TRANSPARENT = 0;

    private float blurRadius = DEFAULT_BLUR_RADIUS;

    private final BlurAlgorithm blurAlgorithm;
    private final float scaleFactor;
    private final boolean applyNoise;
    private BlurViewCanvas internalCanvas;
    private Bitmap internalBitmap;

    @SuppressWarnings("WeakerAccess")
    final View blurView;
    private int overlayColor;
    private final ViewGroup rootView;
    private final int[] rootLocation = new int[2];
    private final int[] blurViewLocation = new int[2];

    @Nullable
    private final Drawable maskDrawable;

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            updateBlur();
            return true;
        }
    };

    private boolean blurEnabled = true;
    private boolean initialized;

    @Nullable
    private Drawable frameClearDrawable;

    public GradientPreDrawBlurController(@NonNull View blurView,
                                         @NonNull ViewGroup rootView,
                                         @ColorInt int overlayColor,
                                         BlurAlgorithm algorithm,
                                         float scaleFactor,
                                         boolean applyNoise,
                                         @Nullable Drawable maskDrawable) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.overlayColor = overlayColor;
        this.blurAlgorithm = algorithm;
        this.scaleFactor = scaleFactor;
        this.applyNoise = applyNoise;
        this.maskDrawable = maskDrawable;

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    void init(int measuredWidth, int measuredHeight) {
        setBlurAutoUpdate(true);
        SizeScaler sizeScaler = new SizeScaler(scaleFactor);
        if (sizeScaler.isZeroSized(measuredWidth, measuredHeight)) {
            blurView.setWillNotDraw(true);
            return;
        }

        blurView.setWillNotDraw(false);
        SizeScaler.Size bitmapSize = sizeScaler.scale(measuredWidth, measuredHeight);
        internalBitmap = Bitmap.createBitmap(bitmapSize.width, bitmapSize.height, blurAlgorithm.getSupportedBitmapConfig());
        internalCanvas = new BlurViewCanvas(internalBitmap);
        initialized = true;
        updateBlur();
    }

    void updateBlur() {
        if (!blurEnabled || !initialized) return;

        if (frameClearDrawable == null) {
            internalBitmap.eraseColor(Color.TRANSPARENT);
        } else {
            frameClearDrawable.draw(internalCanvas);
        }

        internalCanvas.save();
        setupInternalCanvasMatrix();
        try {
            rootView.draw(internalCanvas);
        } catch (Exception e) {
            Log.e("BlurView", "Error during snapshot capturing", e);
        }
        internalCanvas.restore();

        blurAndSave();
    }

    private void setupInternalCanvasMatrix() {
        rootView.getLocationOnScreen(rootLocation);
        blurView.getLocationOnScreen(blurViewLocation);

        int left = blurViewLocation[0] - rootLocation[0];
        int top = blurViewLocation[1] - rootLocation[1];

        float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();

        float scaledLeftPosition = -left / scaleFactorW;
        float scaledTopPosition = -top / scaleFactorH;

        internalCanvas.translate(scaledLeftPosition, scaledTopPosition);
        internalCanvas.scale(1 / scaleFactorW, 1 / scaleFactorH);
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (!blurEnabled || !initialized) return true;
        if (canvas instanceof BlurViewCanvas) return false;

        float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();

        if (maskDrawable == null) {
            canvas.save();
            canvas.scale(scaleFactorW, scaleFactorH);
            blurAlgorithm.render(canvas, internalBitmap);
            canvas.restore();
        } else {
            // Draw blurred content into an offscreen layer, then apply mask as DST_IN
            int w = internalBitmap.getWidth();
            int h = internalBitmap.getHeight();

            canvas.save();
            canvas.scale(scaleFactorW, scaleFactorH);

            // create offscreen layer in internal bitmap coordinate space
            int layer = canvas.saveLayer(0, 0, w, h, null);
            blurAlgorithm.render(canvas, internalBitmap);

            Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

            // Draw mask drawable scaled to internal bitmap size
            if (maskDrawable != null) {
                maskDrawable.setBounds(0, 0, w, h);
                // draw mask with DST_IN to keep only masked alpha region of blurred content
                maskDrawable.draw(canvas);
            }

            maskPaint.setXfermode(null);
            canvas.restoreToCount(layer);
            canvas.restore();
        }

        if (applyNoise) {
            Noise.apply(canvas, blurView.getContext(), blurView.getWidth(), blurView.getHeight());
        }
        if (overlayColor != TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
        return true;
    }

    private void blurAndSave() {
        internalBitmap = blurAlgorithm.blur(internalBitmap, blurRadius);
        if (!blurAlgorithm.canModifyBitmap()) {
            internalCanvas.setBitmap(internalBitmap);
        }
    }

    @Override
    public void updateBlurViewSize() {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();
        init(measuredWidth, measuredHeight);
    }

    @Override
    public void destroy() {
        setBlurAutoUpdate(false);
        blurAlgorithm.destroy();
        initialized = false;
    }

    @Override
    public BlurViewFacade setBlurRadius(float radius) {
        this.blurRadius = radius;
        return this;
    }

    @Override
    public BlurViewFacade setFrameClearDrawable(@Nullable Drawable frameClearDrawable) {
        this.frameClearDrawable = frameClearDrawable;
        return this;
    }

    @Override
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        this.blurEnabled = enabled;
        setBlurAutoUpdate(enabled);
        blurView.invalidate();
        return this;
    }

    public BlurViewFacade setBlurAutoUpdate(final boolean enabled) {
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
            if (rootView.getWindowId() != blurView.getWindowId()) {
                blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
            }
        }
        return this;
    }

    @Override
    public BlurViewFacade setOverlayColor(int overlayColor) {
        if (this.overlayColor != overlayColor) {
            this.overlayColor = overlayColor;
            blurView.invalidate();
        }
        return this;
    }
}
