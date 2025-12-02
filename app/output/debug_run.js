const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

// HELPER FUNCTIONS
function readI32(memory, addr) {
    return new DataView(memory.buffer).getInt32(addr, true);
}

function readF64(memory, addr) {
    return new DataView(memory.buffer).getFloat64(addr, true);
}

function dumpBytes(memory, start, length) {
    const view = new Uint8Array(memory.buffer, start, length);
    console.log([...view]);
}

function debug(memory) {
    const MEMORY_POINTER_ADDR = 1;
    const FRAME_L_ADDR        = MEMORY_POINTER_ADDR + 4;
    const FRAME_R_ADDR        = FRAME_L_ADDR + 4;
    const RESERVED_F64_ADDR   = FRAME_R_ADDR + 4;
    const RESERVED_I32_ADDR   = RESERVED_F64_ADDR + 8;
    const MEMORY_BEGIN        = RESERVED_I32_ADDR + 4;

    const mem = new Uint8Array(memory.buffer);

    console.log("=== MEMORY CONSTANT VALUES ===");
    console.log(`MEMORY_POINTER_ADDR (${MEMORY_POINTER_ADDR}) = ${readI32(mem, MEMORY_POINTER_ADDR)}`);
    console.log(`FRAME_L_ADDR        (${FRAME_L_ADDR})        = ${readI32(mem, FRAME_L_ADDR)}`);
    console.log(`FRAME_R_ADDR        (${FRAME_R_ADDR})        = ${readI32(mem, FRAME_R_ADDR)}`);
    console.log(`RESERVED_F64_ADDR   (${RESERVED_F64_ADDR})   = ${readF64(mem, RESERVED_F64_ADDR)}`);
    console.log(`RESERVED_I32_ADDR   (${RESERVED_I32_ADDR})   = ${readI32(mem, RESERVED_I32_ADDR)}`);
    console.log(`MEMORY_BEGIN        (${MEMORY_BEGIN})        = <start of main memory>`);

    console.log("\n=== FIRST 128 BYTES OF MAIN MEMORY ===");
    dumpBytes(memory, MEMORY_BEGIN, 128);

    const frameLValue = readI32(mem, FRAME_L_ADDR);

    console.log(`\n=== 128 BYTES FROM ADDRESS ${frameLValue} ===`);
    dumpBytes(memory, frameLValue, 128);
 }


(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { , memory } = instance.exports;

})();
