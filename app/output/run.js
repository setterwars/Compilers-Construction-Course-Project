const fs = require('fs');
const path = require('path');

const wasmPath = path.join(__dirname, 'program.wasm');
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { main, memory } = instance.exports;
    const result = main(10, 50, 20, 67, 10000);
    console.log('Result:', result);
})();
