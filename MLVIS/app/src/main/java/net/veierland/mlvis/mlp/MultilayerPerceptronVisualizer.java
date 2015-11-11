package net.veierland.mlvis.mlp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Type;
import android.util.Log;

import net.veierland.mlvis.Visualizer;

public class MultilayerPerceptronVisualizer extends Visualizer {

    private MultilayerPerceptron mModel;
    private String               mDescription;
    private String[]             mLabels;

    private Allocation mInputsAllocation;
    private float[]    mInputs;
    private float[]    mOutputs;

    private float mMaxLabelWidth      = 0.0f;
    private float mMaxScoreLabelWidth = 0.0f;
    private float mBarHeight          = 5.0f;
    private float mBarWidth           = 150.0f;

    private Paint mTextFillPaint;
    private Paint mTextOutlinePaint;
    private Paint mTargetFillPaint;
    private Paint mTargetOutlinePaint;

    private int mTargetImageStride;

    public MultilayerPerceptronVisualizer(Context context, Object model) {
        super(context);

        mTargetImageStride = 8;

        mModel       = (MultilayerPerceptron) model;
        mDescription = mModel.toString();
        mLabels      = mModel.getLastLayer().getLabels();

        RenderScript renderScript = mModel.getRenderScript();
        Type.Builder builder = new Type.Builder(renderScript, Element.F32(renderScript));
        final Type inputsType = builder.setX(1).setY(mModel.getInputSize() + 1).create();
        mInputsAllocation = Allocation.createTyped(renderScript, inputsType);

        mInputs  = new float[mModel.getInputSize() + 1];
        mOutputs = new float[mModel.getLastLayer().getLayerSize() + 1];

        mInputs[0] = 1.0f; // First input is always 1 due to bias in weight matrix

        initializeDrawingObjects();
    }

    private void drawDescription(Canvas canvas) {
        if (mDescription != null) {
            final float distance = (-mTextOutlinePaint.ascent() + mTextOutlinePaint.descent()) * 1.2f;
            final float vertical = canvas.getHeight() - distance;
            canvas.drawText(mDescription, distance, vertical, mTextOutlinePaint);
            canvas.drawText(mDescription, distance, vertical, mTextFillPaint);
        }
    }

    private void drawModelInputs(Canvas canvas) {
        final int[] inputDimensions = mModel.getInputDimensions();

        final int targetWidth  = inputDimensions[1] * mTargetImageStride;
        final int targetHeight = inputDimensions[0] * mTargetImageStride;

        final int horizontalMiddle = canvas.getWidth() / 2;
        final int verticalMiddle   = canvas.getHeight() / 2;

        final int left = horizontalMiddle - targetWidth / 2;
        final int top  = verticalMiddle  - targetHeight / 2;

        for (int i = 1; i < mInputs.length; ++i) {
            int x = (i - 1) % inputDimensions[1];
            int y = (i - 1) / inputDimensions[1];

            int color = (int)(255.0f * mInputs[i]);

            mTargetFillPaint.setColor(Color.rgb(color, color, color));

            canvas.drawRect(left + x * mTargetImageStride,
                    top + y * mTargetImageStride,
                    left + (x + 1) * mTargetImageStride,
                    top + (y + 1) * mTargetImageStride,
                    mTargetFillPaint);
        }

        canvas.drawRect(left, top, left + targetWidth, top + targetHeight, mTargetOutlinePaint);
    }

    private void drawModelOutputs(Canvas canvas) {
        final int   verticalMiddle = canvas.getHeight() / 2;
        final float textSep        = -mTextFillPaint.ascent() + mTextFillPaint.descent();

        float y = verticalMiddle - (mOutputs.length - 1) * textSep / 2.0f;

        for (int i = 1; i < mOutputs.length; ++i) {
            if (mLabels != null) {
                canvas.drawText(mLabels[i - 1], 30.0f, y, mTextOutlinePaint);
                canvas.drawText(mLabels[i - 1], 30.0f, y, mTextFillPaint);
            }

            final String scoreText = String.format("%.2f", mOutputs[i]);

            canvas.drawText(scoreText, 30.0f + mMaxLabelWidth + 15.0f, y, mTextOutlinePaint);
            canvas.drawText(scoreText, 30.0f + mMaxLabelWidth + 15.0f, y, mTextFillPaint);

            canvas.drawRect(
                    30.0f + mMaxLabelWidth + 15.0f + mMaxScoreLabelWidth + 10.0f,
                    y - textSep / 2.0f - mBarHeight / 2.0f + 5.0f,
                    30.0f + mMaxLabelWidth + 15.0f + mMaxScoreLabelWidth + 10.0f + mBarWidth * mOutputs[i],
                    y - textSep / 2.0f + mBarHeight / 2.0f + 5.0f, mTextFillPaint);

            y += textSep;
        }
    }

    private void initializeDrawingObjects() {
        Typeface typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);

        mTextFillPaint = new Paint();
        mTextFillPaint.setAntiAlias(true);
        mTextFillPaint.setDither(true);
        mTextFillPaint.setTextSize(30);
        mTextFillPaint.setTypeface(typeface);
        mTextFillPaint.setColor(Color.WHITE);
        mTextFillPaint.setStyle(Paint.Style.FILL);

        mTextOutlinePaint = new Paint();
        mTextOutlinePaint.setAntiAlias(true);
        mTextOutlinePaint.setDither(true);
        mTextOutlinePaint.setTextSize(30);
        mTextOutlinePaint.setTypeface(typeface);
        mTextOutlinePaint.setColor(Color.BLACK);
        mTextOutlinePaint.setStrokeWidth(2);
        mTextOutlinePaint.setStyle(Paint.Style.STROKE);

        mTargetFillPaint = new Paint();
        mTargetFillPaint.setColor(Color.WHITE);
        mTargetFillPaint.setStrokeWidth(1);
        mTargetFillPaint.setStyle(Paint.Style.FILL);

        mTargetOutlinePaint = new Paint();
        mTargetOutlinePaint.setColor(Color.WHITE);
        mTargetOutlinePaint.setStrokeWidth(1);
        mTargetOutlinePaint.setStyle(Paint.Style.STROKE);

        float maxLabelWidth = 0.0f;
        if (mLabels != null) {
            for (String label : mLabels) {
                maxLabelWidth = Math.max(maxLabelWidth, mTextFillPaint.measureText(label));
            }
        }
        mMaxLabelWidth = maxLabelWidth;

        mMaxScoreLabelWidth = mTextFillPaint.measureText("0.00");
    }

    @Override
    public void onPreviewFrame (byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();

        int horizontalMiddle = size.width / 2;
        int verticalMiddle = size.height / 2;

        int[] inputDimensions = mModel.getInputDimensions();

        int targetWidth  = inputDimensions[1] * mTargetImageStride;
        int targetHeight = inputDimensions[0] * mTargetImageStride;

        int left = horizontalMiddle - targetWidth / 2;
        int top = verticalMiddle - targetHeight / 2;

        for (int x = 0; x < inputDimensions[1]; ++x) {
            for (int y = 0; y < inputDimensions[0]; ++y) {
                int value = 0;
                for (int sx = 0; sx < mTargetImageStride; ++sx) {
                    for (int sy = 0; sy < mTargetImageStride; ++sy) {
                        switch (mDisplayOrientation) {
                            case  90: value += data[(top + targetWidth - (x * mTargetImageStride + sx)) * size.width + left + y * mTargetImageStride + sy] & 0xFF; break;
                            case 180: value += data[(top + targetHeight - (y * mTargetImageStride + sy)) * size.width + left + targetWidth - (x * mTargetImageStride + sx)] & 0xFF; break;
                            case 270: value += data[(top + targetWidth - (x * mTargetImageStride + sx)) * size.width + left + targetHeight - (y * mTargetImageStride + sy)] & 0xFF; break;
                            default:  value += data[(top + y * mTargetImageStride + sy) * size.width + left + x * mTargetImageStride + sx] & 0xFF; break;
                        }
                    }
                }
                // NB: Very primitive filter. To be replaced:
                final float v = Math.max(0.0f, 1.0f - 2.0f * value / (255.0f * mTargetImageStride * mTargetImageStride));
                mInputs[1 + y * inputDimensions[0] + x] = v;
            }
        }

        mInputsAllocation.copy2DRangeFrom(0, 0, 1, mInputs.length, mInputs);
        Allocation result = mModel.evaluate(mInputsAllocation);
        result.copy2DRangeTo(0, 0, 1, mOutputs.length, mOutputs);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawDescription(canvas);
        drawModelInputs(canvas);
        drawModelOutputs(canvas);
    }
}
