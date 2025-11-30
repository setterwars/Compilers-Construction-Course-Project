const fs = require('fs');
const path = require('path');

const wasmPath = path.join(__dirname, 'program.wasm');
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { euclideanDistance, memory } = instance.exports;
    const result = euclideanDistance(1, 1, 4, 5);
    console.log('Result:', result);
})();
