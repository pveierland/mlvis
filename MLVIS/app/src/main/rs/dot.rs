// https://gaipaul.wordpress.com/2014/12/12/arbitrary-sized-matrix-multiplication-using-renderscript/
#pragma version(1)
#pragma rs java_package_name(net.veierland.mlvis.mlp)

uint32_t N;

rs_allocation gA;
rs_allocation gB;
rs_allocation gC;

void evaluate(float *v_out, uint32_t x, uint32_t y) {
    float sum = 0.0f;

    for (uint32_t c = 0; c < N; ++c) {
        float r = rsGetElementAt_float(gA, c, y);
        float s = rsGetElementAt_float(gB, x, c);
        sum += r * s;
    }

    rsSetElementAt_float(gC, sum, x, y);
}
