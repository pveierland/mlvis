package net.veierland.mlvis.mlp;

import android.content.Context;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;

public class MultilayerPerceptron {

    public static MultilayerPerceptron fromJson(Context    context,
                                                JsonReader jsonReader) throws IOException {
        int[] inputDimensions = null;
        int[] layerSizes      = null;

        jsonReader.beginArray();

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if (name.equals("input_dimensions")) {
                inputDimensions = parseIntegerList(jsonReader);
            } else if (name.equals("layer_sizes")) {
                layerSizes = parseIntegerList(jsonReader);
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        RenderScript renderScript = RenderScript.create(context);

        ArrayList<PerceptronLayer> layers = new ArrayList<PerceptronLayer>();

        jsonReader.beginArray();
        for (int layerIndex = 0; jsonReader.hasNext(); ++layerIndex) {
            final int inputSize = layerIndex == 0
                ? multiplyIntegers(inputDimensions) : layerSizes[layerIndex - 1];

            layers.add(PerceptronLayer.fromJson(
                renderScript, jsonReader, inputSize, layerSizes[layerIndex]));
        }
        jsonReader.endArray();

        jsonReader.endArray();

        return new MultilayerPerceptron(renderScript, inputDimensions, layerSizes, layers);
    }

    private static int multiplyIntegers(int[] integers) {
        int product = 1;
        for (int i : integers) {
            product *= i;
        }
        return product;
    }

    private static int[] parseIntegerList(JsonReader jsonReader) throws IOException {
        ArrayList<Integer> values = new ArrayList<Integer>();

        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            values.add(jsonReader.nextInt());
        }
        jsonReader.endArray();

        int[] integers = new int[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            integers[i] = values.get(i);
        }

        return integers;
    }

    private RenderScript               mRS;
    private int[]                      mInputDimensions;
    private int[]                      mLayerSizes;
    private ArrayList<PerceptronLayer> mLayers;

    public MultilayerPerceptron(RenderScript               renderScript,
                                int[]                      inputDimensions,
                                int[]                      layerSizes,
                                ArrayList<PerceptronLayer> layers) {
        mRS              = renderScript;
        mInputDimensions = inputDimensions;
        mLayerSizes      = layerSizes;
        mLayers          = layers;
    }

    public Allocation evaluate(Allocation value) {
        for (PerceptronLayer layer : mLayers) {
            value = layer.evaluate(value);
        }
        return value;
    }

    public int[] getInputDimensions() {
        return mInputDimensions;
    }

    public int getInputSize() {
        return multiplyIntegers(mInputDimensions);
    }

    public PerceptronLayer getLastLayer() {
        return mLayers.get(mLayers.size() - 1);
    }

    public int[] getLayerSizes() {
        return mLayerSizes;
    }

    public RenderScript getRenderScript() {
        return mRS;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("I: ");

        for (int i = 0; i < mInputDimensions.length; ++i) {
            if (i != 0) {
                sb.append("x");
            }
            sb.append(mInputDimensions[i]);
        }

        sb.append(" ");

        for (int i = 0; i < mLayers.size(); ++i) {
            sb.append(i != mLayers.size() - 1 ? "H: " : "O: ");
            sb.append(mLayers.get(i).getLayerSize());
            sb.append(" (");
            sb.append(mLayers.get(i).getActivationFunction().toString().toUpperCase());
            sb.append(") ");
        }

        return sb.toString();
    }
}
