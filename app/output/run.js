const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

// Constants
const MEMORY_POINTER_ADDR = 1;
const FRAME_L_ADDR        = MEMORY_POINTER_ADDR + 4;     // 5
const FRAME_R_ADDR        = FRAME_L_ADDR + 4;            // 9
const RESERVED_F64_ADDR   = FRAME_R_ADDR + 4;            // 13
const RESERVED_I32_ADDR   = RESERVED_F64_ADDR + 8;        // 21
const MEMORY_BEGIN        = RESERVED_I32_ADDR + 4;        // 25

function readI32(mem, addr) {
    return new DataView(mem.buffer).getInt32(addr, true);
}

function readF64(mem, addr) {
    return new DataView(mem.buffer).getFloat64(addr, true);
}

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { main, memory } = instance.exports;

    // run program
    const result = main(
        1.0, 10.0, 100.0, 1000.0,
        10000.0, 1000000.0, 10000000.0,
        100000000.0, 1000000000.0
    );
    console.log("result: ", result)

    const mem = new Uint8Array(memory.buffer);

    console.log("=== MEMORY CONSTANT VALUES ===");
    console.log(`__DEBUG_REMOVE = ${readI32(mem, 1)}`);
    console.log(`MEMORY_POINTER_ADDR (${MEMORY_POINTER_ADDR}) = ${readI32(mem, MEMORY_POINTER_ADDR)}`);
    console.log(`FRAME_L_ADDR        (${FRAME_L_ADDR})        = ${readI32(mem, FRAME_L_ADDR)}`);
    console.log(`FRAME_R_ADDR        (${FRAME_R_ADDR})        = ${readI32(mem, FRAME_R_ADDR)}`);
    console.log(`RESERVED_F64_ADDR   (${RESERVED_F64_ADDR})   = ${readF64(mem, RESERVED_F64_ADDR)}`);
    console.log(`RESERVED_I32_ADDR   (${RESERVED_I32_ADDR})   = ${readI32(mem, RESERVED_I32_ADDR)}`);
    console.log(`MEMORY_BEGIN        (${MEMORY_BEGIN})        = <start of main memory>`);

    console.log("\n=== FIRST 128 BYTES OF MAIN MEMORY ===");
    const mainMem = Array.from(mem.slice(MEMORY_BEGIN, MEMORY_BEGIN + 128));
    console.log(mainMem);

    // ---------------------------------------
    // NEW: Print bytes starting at FRAME_L_ADDR
    // ---------------------------------------
    const frameLValue = readI32(mem, FRAME_L_ADDR);

    console.log("\n=== FRAME L BLOCK ===");
    console.log(`FRAME_L value = ${frameLValue}`);

    console.log(`\n=== 128 BYTES FROM ADDRESS ${frameLValue} ===`);
    const frameLMem = Array.from(mem.slice(frameLValue, frameLValue + 128));
    console.log(frameLMem);
})();
