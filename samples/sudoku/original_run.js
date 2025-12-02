const fs = require("fs");
const path = require("path");

const wasmPath = path.join(__dirname, "program.wasm");
const bytes = fs.readFileSync(wasmPath);

function readI32(memory, addr) {
    return new DataView(memory.buffer).getInt32(addr, true); // little-endian
}

function writeGrid(grid, makeGridFromArray, __allocate_i32_array, __write_i32_to_array) {
    const flattedGrid = grid.flat();
    const addr = __allocate_i32_array(81);
    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            __write_i32_to_array(addr, 9 * i + j, grid[i][j]);
        }
    }
    const createdAddress = makeGridFromArray(addr);
    return createdAddress;
}

function readGrid(grid, makeArrayFromGrid, memory) {
    const addr = makeArrayFromGrid(grid)
    const result = Array.from({ length: 9 }, () => Array(9).fill(0));

    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            result[i][j] = readI32(memory, addr + (9 * i + j) * 4)
        }
    }

    return result
}

(async () => {
    const { instance } = await WebAssembly.instantiate(bytes, {});
    const { makeGridFromArray, makeArrayFromGrid, solveSudoku, __allocate_i32_array, __write_i32_to_array, memory } = instance.exports;
//
//    const matrix = [
//      [6, 2, 3, 5, 0, 0, 4, 0, 0],
//      [0, 9, 0, 0, 8, 7, 0, 3, 2],
//      [8, 0, 0, 0, 3, 2, 5, 0, 1],
//
//      [7, 0, 8, 2, 0, 4, 9, 0, 0],
//      [4, 0, 9, 7, 0, 0, 0, 2, 6],
//      [0, 6, 0, 3, 5, 0, 8, 7, 0],
//
//      [0, 0, 0, 8, 4, 6, 7, 0, 3],
//      [3, 8, 6, 0, 0, 0, 0, 4, 9],
//      [0, 7, 4, 0, 2, 3, 0, 6, 0]
//    ];
//
//    const matrix = [
//      [0, 0, 7, 0, 0, 2, 8, 1, 6],
//      [1, 8, 0, 5, 3, 0, 0, 4, 0],
//      [9, 0, 2, 1, 8, 0, 0, 0, 3],
//
//      [0, 0, 4, 7, 0, 1, 3, 0, 5],
//      [6, 1, 0, 0, 0, 5, 4, 0, 7],
//      [0, 5, 3, 0, 2, 8, 0, 6, 0],
//
//      [8, 0, 5, 6, 7, 3, 0, 0, 0],
//      [4, 7, 0, 0, 0, 0, 6, 3, 8],
//      [0, 6, 0, 8, 1, 0, 7, 5, 0]
//    ];

//    const matrix = [
//      [8, 1, 7, 0, 0, 0, 0, 4, 5],
//      [0, 0, 0, 0, 5, 1, 7, 0, 6],
//      [2, 6, 5, 0, 0, 3, 0, 0, 1],
//      [4, 7, 0, 5, 6, 8, 0, 0, 0],
//      [9, 5, 1, 0, 0, 0, 0, 8, 0],
//      [0, 3, 0, 0, 9, 0, 2, 0, 0],
//      [0, 4, 0, 2, 0, 0, 0, 0, 0],
//      [0, 0, 0, 0, 0, 5, 0, 7, 9],
//      [5, 8, 9, 7, 3, 0, 1, 6, 0]
//    ];

    let g = writeGrid(matrix, makeGridFromArray, __allocate_i32_array, __write_i32_to_array);
    let sudokuSolveResult = solveSudoku(g);
    const result = readGrid(g, makeArrayFromGrid, memory);
    result.forEach(row => console.log(row.join(" ")));
})();
