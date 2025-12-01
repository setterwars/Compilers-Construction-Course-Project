let wasmAdd = null;

async function loadWasm() {
  const response = await fetch('add.wasm');
  const bytes = await response.arrayBuffer();
  const { instance } = await WebAssembly.instantiate(bytes);
  wasmAdd = instance.exports.add;
}

loadWasm();

document.getElementById('sumBtn').addEventListener('click', () => {
  if (!wasmAdd) {
    alert('WASM not loaded yet, try again in a moment.');
    return;
  }

  const aValue = document.getElementById('a').value || '0';
  const bValue = document.getElementById('b').value || '0';

  const a = parseInt(aValue, 10);
  const b = parseInt(bValue, 10);

  const result = wasmAdd(a, b);

  document.getElementById('result').textContent = ' = ' + result;
});
