const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { countDivisors } = instance.exports;
    for (let i = 1; i <= 20; i++) {
        console.log(i, countDivisors(i));
    }
})();
