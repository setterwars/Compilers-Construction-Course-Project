const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { buildMatrix, computeDeterminant, __allocate_f64_array, __write_f64_to_array, memory } = instance.exports;

    let n = 3;
    const matrixFlattened = __allocate_f64_array(n * n)
    const numbers = [0, 1, 2, 3, 4, 5, 6, -10, 8];
    for (let i = 0; i < n * n; i++) {
      __write_f64_to_array(matrixFlattened, i, numbers[i]);
    }
    const matrix = buildMatrix(n, matrixFlattened);
    const determinant = computeDeterminant(n, matrix);
    console.log("matrix determinant = ", determinant);
})();
