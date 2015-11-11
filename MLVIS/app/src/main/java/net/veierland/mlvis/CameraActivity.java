package net.veierland.mlvis;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import net.veierland.mlvis.mlp.MultilayerPerceptron;
import net.veierland.mlvis.mlp.MultilayerPerceptronVisualizer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.List;

public class CameraActivity extends Activity  implements SurfaceHolder.Callback {

    public static final String TAG = CameraActivity.class.getSimpleName();

    private static final int LOAD_MODEL_FILE = 42;

    private int mDisplayRotation;
    private int mDisplayOrientation;
    private int mOrientation;
    private int mOrientationCompensation;

    private OrientationEventListener mOrientationEventListener;

    private Camera      mCamera;
    private SurfaceView mView;
    private Visualizer  mVisualizer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new SurfaceView(this);

        setContentView(mView);

        // Create and Start the OrientationListener:
        mOrientationEventListener = new SimpleOrientationEventListener(this);
        mOrientationEventListener.enable();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, LOAD_MODEL_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOAD_MODEL_FILE && resultCode == Activity.RESULT_OK) {
            new LoadModelTask().execute(data.getData());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        mOrientationEventListener.disable();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mOrientationEventListener.enable();
        super.onResume();
    }

    private void setVisualizer(Visualizer visualizer) {
        if (mVisualizer != null) {
            ((ViewGroup) mVisualizer.getParent()).removeView(mVisualizer);
            mVisualizer = null;
        }

        addContentView(visualizer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        visualizer.setDisplayOrientation(mDisplayOrientation);
        visualizer.setOrientation(mOrientation);

        mCamera.setPreviewCallback(visualizer);
        mVisualizer = visualizer;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) { }

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);

        parameters.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(parameters);

        mDisplayRotation    = Util.getDisplayRotation(CameraActivity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, 0);
        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mVisualizer != null)
        {
            mVisualizer.setDisplayOrientation(mDisplayOrientation);
        }

        if (mVisualizer != null) {
            mCamera.setPreviewCallback(mVisualizer);
        }

        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallback(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    /**
     * We need to react on OrientationEvents to rotate the screen and
     * update the views.
     */
    private class SimpleOrientationEventListener extends OrientationEventListener {

        public SimpleOrientationEventListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(CameraActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                mVisualizer.setOrientation(mOrientationCompensation);
            }
        }
    }

    private static class LoadModelTaskResult {
        public Object model        = null;
        public Class  visualizer   = null;
        public String toastMessage = null;
    }

    private class LoadModelTask extends AsyncTask<Uri, String, LoadModelTaskResult> {

        private ProgressDialog mProgressDialog;

        @Override
        protected LoadModelTaskResult doInBackground(Uri... models) {
            LoadModelTaskResult result = new LoadModelTaskResult();

            publishProgress(models[0].toString());

            try {
                InputStream inputStream = CameraActivity.this.getContentResolver().openInputStream(models[0]);
                //InputStream inputStream = CameraActivity.this.getResources().openRawResource(R.raw.mlvis);

                JsonReader jsonReader = new JsonReader(new InputStreamReader(Util.decompressStream(inputStream)));

                jsonReader.beginArray();

                String type = "";

                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("type")) {
                        type = jsonReader.nextString();
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();

                if (type.equals("multilayer_perceptron")) {
                    result.visualizer = MultilayerPerceptronVisualizer.class;
                    result.model      = MultilayerPerceptron.fromJson(CameraActivity.this, jsonReader);
                }

                jsonReader.endArray();
            }
            catch (RuntimeException exception) {
                result.toastMessage = String.format("Invalid model input: %s", exception.getMessage());
            }
            catch (FileNotFoundException exception) {
                result.toastMessage = String.format("Failed to find model file: %s", exception.getMessage());
            }
            catch (Exception exception) {
                result.toastMessage = String.format("Failed to load model file: %s", exception.getMessage());
            }

            return result;
        }

        protected void onPostExecute(LoadModelTaskResult result) {
            mProgressDialog.dismiss();

            try {
                if (result.visualizer != null && result.model != null) {
                    Constructor visualizerConstructor = result.visualizer.getConstructor(
                            new Class[]{Context.class, Object.class});

                    CameraActivity.this.setVisualizer(
                        (Visualizer) visualizerConstructor.newInstance(
                                CameraActivity.this, result.model));
                }
            } catch (Exception e) {
                result.toastMessage = e.getMessage();
            }

            if (result.toastMessage != null) {
                Toast.makeText(CameraActivity.this, result.toastMessage, Toast.LENGTH_LONG).show();
            }
        }

        protected void onProgressUpdate(String... progress) {
            mProgressDialog = ProgressDialog.show(
                CameraActivity.this, progress[0], "Parsing model file...", true, false);
        }
    }
}
