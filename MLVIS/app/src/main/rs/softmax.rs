#pragma version(1)
#pragma rs java_package_name(net.veierland.mlvis.mlp)

uint32_t K;

rs_allocation gZ;
rs_allocation gS;

void evaluate(float *v_out, uint32_t x, uint32_t y) {
    // Ignore bias input value
    if (y != 0) {
        float sum = 0.0f;

        // Doing a sum per thread is likely horrible
        for (uint32_t k = 1; k < K; ++k) {
            float z_k = rsGetElementAt_float(gZ, 0, k);
            sum += exp(z_k);
        }

        float z_j = rsGetElementAt_float(gZ, x, y);

        rsSetElementAt_float(gS, exp(z_j) / sum, x, y);
    }
}
