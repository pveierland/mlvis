package net.veierland.mlvis;

import android.content.Context;
import android.hardware.Camera;
import android.view.View;

public abstract class Visualizer extends View implements Camera.PreviewCallback {

    protected int mDisplayOrientation;
    protected int mOrientation;

    public Visualizer(Context context) {
        super(context);
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }
}
