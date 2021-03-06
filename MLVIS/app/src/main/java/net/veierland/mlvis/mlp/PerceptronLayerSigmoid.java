package net.veierland.mlvis.mlp;

import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Type;

public class PerceptronLayerSigmoid extends PerceptronLayer
{
    private Allocation      mWeights;
    private Allocation      mResult;
    private ScriptC_dot     mScriptDot;
    private ScriptC_sigmoid mScriptSigmoid;

    public PerceptronLayerSigmoid(RenderScript        renderScript,
                                  int                 inputSize,
                                  int                 layerSize,
                                  ActivationFunction  activationFunction,
                                  float[]             weights,
                                  String[]            labels)
    {
        super(renderScript, inputSize, layerSize, activationFunction, labels);

        Type.Builder builder = new Type.Builder(renderScript, Element.F32(renderScript));

        final Type weightsType = builder.setX(inputSize + 1).setY(layerSize + 1).create();
        mWeights = Allocation.createTyped(renderScript, weightsType);
        mWeights.copy2DRangeFrom(0, 0, inputSize + 1, layerSize + 1, weights);

        final Type resultType = builder.setX(1).setY(layerSize + 1).create();
        mResult = Allocation.createTyped(renderScript, resultType);

        mScriptDot = new ScriptC_dot(renderScript);
        mScriptDot.set_N(inputSize + 1);
        mScriptDot.set_gA(mWeights);
        mScriptDot.set_gC(mResult);

        mScriptSigmoid = new ScriptC_sigmoid(renderScript);
    }

    public Allocation evaluate(Allocation input) {
        mScriptDot.set_gB(input);
        mScriptDot.forEach_evaluate(mResult);
        mScriptSigmoid.forEach_evaluate(mResult);
        return mResult;
    }
}
