const PUZZLE_1 = [
    [6, 2, 3, 5, 0, 0, 4, 0, 0],
    [0, 9, 0, 0, 8, 7, 0, 3, 2],
    [8, 0, 0, 0, 3, 2, 5, 0, 1],

    [7, 0, 8, 2, 0, 4, 9, 0, 0],
    [4, 0, 9, 7, 0, 0, 0, 2, 6],
    [0, 6, 0, 3, 5, 0, 8, 7, 0],

    [0, 0, 0, 8, 4, 6, 7, 0, 3],
    [3, 8, 6, 0, 0, 0, 0, 4, 9],
    [0, 7, 4, 0, 2, 3, 0, 6, 0]
];

const PUZZLE_2 = [
    [0, 0, 7, 0, 0, 2, 8, 1, 6],
    [1, 8, 0, 5, 3, 0, 0, 4, 0],
    [9, 0, 2, 1, 8, 0, 0, 0, 3],

    [0, 0, 4, 7, 0, 1, 3, 0, 5],
    [6, 1, 0, 0, 0, 5, 4, 0, 7],
    [0, 5, 3, 0, 2, 8, 0, 6, 0],

    [8, 0, 5, 6, 7, 3, 0, 0, 0],
    [4, 7, 0, 0, 0, 0, 6, 3, 8],
    [0, 6, 0, 8, 1, 0, 7, 5, 0]
];

const PUZZLE_3 = [
    [8, 1, 7, 0, 0, 0, 0, 4, 5],
    [0, 0, 0, 0, 5, 1, 7, 0, 6],
    [2, 6, 5, 0, 0, 3, 0, 0, 1],

    [4, 7, 0, 5, 6, 8, 0, 0, 0],
    [9, 5, 1, 0, 0, 0, 0, 8, 0],
    [0, 3, 0, 0, 9, 0, 2, 0, 0],

    [0, 4, 0, 2, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 5, 0, 7, 9],
    [5, 8, 9, 7, 3, 0, 1, 6, 0]
];

const inputContainer = document.getElementById("sudoku-input-container");
const solutionContainer = document.getElementById("solution-container");
const statusEl = document.getElementById("status");

let inputCells = [];
let solutionCells = [];

(function createInputGrid() {
    const table = document.createElement("table");
    inputCells = [];

    for (let i = 0; i < 9; i++) {
        const row = document.createElement("tr");
        const rowCells = [];
        for (let j = 0; j < 9; j++) {
            const cell = document.createElement("td");

            if (j % 3 === 0) cell.style.borderLeftWidth = "2px";
            if (i % 3 === 0) cell.style.borderTopWidth = "2px";
            if (j === 8) cell.style.borderRightWidth = "2px";
            if (i === 8) cell.style.borderBottomWidth = "2px";

            const input = document.createElement("input");
            input.type = "text";
            input.maxLength = 1;

            rowCells.push(input);
            cell.appendChild(input);
            row.appendChild(cell);
        }
        inputCells.push(rowCells);
        table.appendChild(row);
    }

    inputContainer.appendChild(table);
})();

(function createSolutionGrid() {
    const table = document.createElement("table");
    solutionCells = [];

    for (let i = 0; i < 9; i++) {
        const row = document.createElement("tr");
        const rowCells = [];
        for (let j = 0; j < 9; j++) {
            const cell = document.createElement("td");

            if (j % 3 === 0) cell.style.borderLeftWidth = "2px";
            if (i % 3 === 0) cell.style.borderTopWidth = "2px";
            if (j === 8) cell.style.borderRightWidth = "2px";
            if (i === 8) cell.style.borderBottomWidth = "2px";

            cell.textContent = "";
            rowCells.push(cell);
            row.appendChild(cell);
        }
        solutionCells.push(rowCells);
        table.appendChild(row);
    }

    solutionContainer.appendChild(table);
})();

function fillInputsFromMatrix(matrix) {
    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            const value = matrix[i][j];
            inputCells[i][j].value = value === 0 ? "" : String(value);
        }
    }
}

document.getElementById("prefill1").addEventListener("click", () => {
    fillInputsFromMatrix(PUZZLE_1);
});

document.getElementById("prefill2").addEventListener("click", () => {
    fillInputsFromMatrix(PUZZLE_2);
});

document.getElementById("prefill3").addEventListener("click", () => {
    fillInputsFromMatrix(PUZZLE_3);
});

function readMatrixFromInputs() {
    const matrix = Array.from({
        length: 9
    }, () => Array(9).fill(0));
    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            const raw = inputCells[i][j].value.trim();
            const num = raw === "" ? 0 : Number(raw);
            matrix[i][j] = Number.isInteger(num) && num >= 1 && num <= 9 ? num : 0;
        }
    }
    return matrix;
}

// WASM STUFF BEGINS HERE
let wasmInstance = null;
let wasmExports = null;

const wasmReady = (async function initWasm() {
    const response = await fetch("program.wasm");
    const bytes = await response.arrayBuffer();
    const {
        instance
    } = await WebAssembly.instantiate(bytes, {});
    wasmInstance = instance;
    wasmExports = instance.exports;
})();

function readI32(addr) {
    const memory = wasmExports.memory;
    return new DataView(memory.buffer).getInt32(addr, true);
}

function writeGridToWasm(grid) {
    const addr = wasmExports.__allocate_i32_array(81);
    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            wasmExports.__write_i32_to_array(addr, 9 * i + j, grid[i][j]);
        }
    }
    const gridHandle = wasmExports.makeGridFromArray(addr);
    return gridHandle;
}

function readGridFromWasm(gridHandle) {
    const addr = wasmExports.makeArrayFromGrid(gridHandle);
    const result = Array.from({
        length: 9
    }, () => Array(9).fill(0));

    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            result[i][j] = readI32(addr + (9 * i + j) * 4);
        }
    }
    return result;
}

function solveSudokuWithWasm(matrix) {
    const gridHandle = writeGridToWasm(matrix);
    const solveResult = wasmExports.solveSudoku(gridHandle);
    const solved = readGridFromWasm(gridHandle);
    return {
        solveResult,
        solved
    };
}

function renderSolution(matrix) {
    for (let i = 0; i < 9; i++) {
        for (let j = 0; j < 9; j++) {
            solutionCells[i][j].textContent = matrix[i][j] || "";
        }
    }
}

document.getElementById("solve").addEventListener("click", async () => {
    statusEl.textContent = "Solving...";
    try {
        await wasmReady;

        const inputMatrix = readMatrixFromInputs();
        const {
            solveResult,
            solved
        } = solveSudokuWithWasm(inputMatrix);

        renderSolution(solved);

        statusEl.textContent =
            typeof solveResult === "number" ?
            `Solved (WASM returned: ${solveResult})` :
            "Solved.";
    } catch (e) {
        console.error(e);
        statusEl.textContent = "Error while solving; see console.";
    }
});