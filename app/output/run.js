const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { calculateSum, __allocate_i32_array, __write_i32_to_array, memory } = instance.exports;

    const n = 10;
    const arrayAddress = __allocate_i32_array(n);
    console.log("arrayAddress: ", arrayAddress);
    for (let i = 0; i < n; i++) {
        __write_i32_to_array(arrayAddress, i, i);
    }
    const result = calculateSum(n, arrayAddress);
    console.log("result: ", result);
})();
