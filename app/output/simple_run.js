const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { main } = instance.exports;
    console.log(main());
})();
