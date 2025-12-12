package eightbitlab.com.blurview;

import static eightbitlab.com.blurview.PreDrawBlurController.TRANSPARENT;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eightbitlab.blurview.R;


/**
 * View that blurs the content behind it using a gradient mask.
 * Use in XML and call {@link #setupWith(BlurTarget)} to attach to a blur target.
 */
public class GradientBlurView extends BlurView {

    private Drawable maskDrawable;
    private float scaleFactor = 0.35f;
    private boolean applyNoise = true;

    public GradientBlurView(@NonNull Context context) {
        this(context, null);
    }

    public GradientBlurView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GradientBlurView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GradientBlurView);
            maskDrawable = a.getDrawable(R.styleable.GradientBlurView_maskDrawable);
            scaleFactor = a.getFloat(R.styleable.GradientBlurView_scaleFactor, scaleFactor);
            applyNoise = a.getBoolean(R.styleable.GradientBlurView_applyNoise, applyNoise);
            int overlayColor = a.getColor(R.styleable.GradientBlurView_overlayColor, TRANSPARENT);
            // apply overlay color to this view's initial overlayColor via setOverlayColor later
            a.recycle();
            setOverlayColor(overlayColor);
        }
    }

    /**
     * Attach blur to a BlurTarget (root) similar to BlurView.setupWith but using gradient mask.
     */
    public BlurViewFacade setupWith(@NonNull BlurTarget target) {
        // Use RenderScript blur algorithm as a general fallback for all API levels
        RenderScriptBlur algorithm = new RenderScriptBlur(getContext());
        float effectiveScale = Math.max(0.1f, Math.min(1f, scaleFactor));
        GradientPreDrawBlurController controller = new GradientPreDrawBlurController(
                this,
                (ViewGroup) target.getRootView(),
                TRANSPARENT,
                algorithm,
                effectiveScale,
                applyNoise,
                maskDrawable
        );
        this.blurController = controller;
        return controller;
    }

}
