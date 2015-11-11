#pragma version(1)
#pragma rs java_package_name(net.veierland.mlvis.mlp)

void evaluate(float *v_out, uint32_t x, uint32_t y) {
    // Ignore bias input value
    *v_out = y != 0 ? max(0.0f, *v_out) : 1.0f;
}
