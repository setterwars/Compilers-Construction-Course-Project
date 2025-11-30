const fs = require('fs');
const path = require('path');

const wasmPath = path.join(__dirname, 'program.wasm');
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { squareRoot, memory } = instance.exports;
    const result = squareRoot(2.0);
    console.log('Result:', result);

    const mem = new Uint8Array(memory.buffer);
    console.log("First 64 bytes of WASM memory:");
    console.log(mem.slice(0, 64));
})();
