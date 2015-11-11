package net.veierland.mlvis.mlp;

import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Allocation;
import android.util.JsonReader;

import java.io.IOException;

public abstract class PerceptronLayer {

    public static PerceptronLayer fromJson(RenderScript renderScript,
                                           JsonReader   jsonReader,
                                           int          inputSize,
                                           int          layerSize) throws IOException {

        ActivationFunction activationFunction = null;
        float[]  weights = new float[(layerSize + 1) * (inputSize + 1)];
        String[] labels  = null;

        weights[0] = 1.0f;

        jsonReader.beginObject();

        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if (name.equals("activation_function")) {
                activationFunction = ActivationFunction.valueOf(jsonReader.nextString());
            } else if (name.equals("bias")) {
                parseBias(jsonReader, weights, inputSize, layerSize);
            } else if (name.equals("weights")) {
                parseWeights(jsonReader, weights, inputSize, layerSize);
            } else if (name.equals("labels")) {
                labels = parseLabels(jsonReader, layerSize);
            } else {
                jsonReader.skipValue();
            }
        }

        jsonReader.endObject();

        switch (activationFunction) {
            case relu:
                return new PerceptronLayerRelu(
                    renderScript, inputSize, layerSize, activationFunction, weights, labels);
            case sigmoid:
                return new PerceptronLayerSigmoid(
                    renderScript, inputSize, layerSize, activationFunction, weights, labels);
            case softmax:
                return new PerceptronLayerSoftmax(
                    renderScript, inputSize, layerSize, activationFunction, weights, labels);
            case tanh:
                return new PerceptronLayerTanh(
                    renderScript, inputSize, layerSize, activationFunction, weights, labels);
            default:
                throw new RuntimeException("unsupported activation function");
        }
    }

    private static void parseBias(JsonReader jsonReader,
                                  float[]    weights,
                                  int        inputSize,
                                  int        layerSize) throws IOException {
        jsonReader.beginArray();
        for (int row = 1; jsonReader.hasNext(); ++row) {
            final int index = row * (inputSize + 1);
            weights[index] = (float) jsonReader.nextDouble();
        }
        jsonReader.endArray();
    }

    private static String[] parseLabels(JsonReader jsonReader, int layerSize) throws IOException {
        String[] labels = new String[layerSize];

        jsonReader.beginArray();
        for (int i = 0; jsonReader.hasNext(); ++i) {
            labels[i] = jsonReader.nextString();
        }
        jsonReader.endArray();

        return labels;
    }

    private static void parseWeights(JsonReader jsonReader,
                                     float[]    weights,
                                     int        inputSize,
                                     int        layerSize) throws IOException {
        jsonReader.beginArray();
        for (int row = 1; jsonReader.hasNext(); ++row) {
            jsonReader.beginArray();
            for (int column = 1; jsonReader.hasNext(); ++column) {
                final int index = row * (inputSize + 1) + column;
                weights[index] = (float) jsonReader.nextDouble();
            }
            jsonReader.endArray();
        }
        jsonReader.endArray();
    }

    protected RenderScript        mRS;
    protected int                 mInputSize;
    protected int                 mLayerSize;
    protected ActivationFunction  mActivationFunction;
    protected String[]            mLabels;

    public PerceptronLayer(RenderScript        renderScript,
                           int                 inputSize,
                           int                 layerSize,
                           ActivationFunction  activationFunction,
                           String[]            labels)
    {
        mRS                 = renderScript;
        mInputSize          = inputSize;
        mLayerSize          = layerSize;
        mActivationFunction = activationFunction;
        mLabels             = labels;
    }

    public abstract Allocation evaluate(Allocation input);

    public ActivationFunction getActivationFunction() {
        return mActivationFunction;
    }

    public int getInputSize() {
        return mInputSize;
    }

    public String[] getLabels() {
        return mLabels;
    }

    public int getLayerSize() {
        return mLayerSize;
    }
}
