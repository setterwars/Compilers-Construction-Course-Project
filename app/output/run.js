const fs = require('fs');
const path = require('path');

const wasmPath = path.join(__dirname, 'program.wasm');
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { euclideanDistance, memory } = instance.exports;
    euclideanDistance(0, 0, 10, 10);
    console.log('euclideanDistance called successfully');

    const mem = new Uint8Array(memory.buffer);
    console.log("First 64 bytes of WASM memory:");
    console.log(mem.slice(0, 64));
})();
