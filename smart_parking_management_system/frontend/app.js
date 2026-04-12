async function fetchJSON(url, options = {}) {
  const response = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  return response.json();
}

function renderTable(el, rows) {
  if (!rows || rows.length === 0) {
    el.innerHTML = '<p>No records found.</p>';
    return;
  }

  const headers = Object.keys(rows[0]);
  const head = `<tr>${headers.map((h) => `<th>${h}</th>`).join("")}</tr>`;
  const body = rows
    .map((row) => `<tr>${headers.map((h) => `<td>${row[h]}</td>`).join("")}</tr>`)
    .join("");

  el.innerHTML = `<table><thead>${head}</thead><tbody>${body}</tbody></table>`;
}
