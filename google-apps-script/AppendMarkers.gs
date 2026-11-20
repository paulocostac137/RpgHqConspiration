const SPREADSHEET_ID = '13ESbous-qqBsQf6VU_6Rw4zb_aoBguosGXY9eh86P9M';
const API_TOKEN = '9718';

function doPost(event) {
  try {
    if (!event || !event.postData || !event.postData.contents) {
      return respond({ ok: false, error: 'Corpo da requisição ausente.' });
    }

    const body = JSON.parse(event.postData.contents);
    if (!body || body.token !== API_TOKEN) {
      return respond({ ok: false, error: 'Token inválido.' });
    }
    if (!Array.isArray(body.rows) || body.rows.length === 0) {
      return respond({ ok: false, error: 'Nenhuma linha recebida.' });
    }

    const rows = body.rows.map(function (row) {
      if (!Array.isArray(row) || row.length !== 4 ||
          row[0] !== '' ||
          typeof row[1] !== 'string' || row[1].trim() === '' ||
          !['G', 'O', 'T'].includes(String(row[2])) ||
          !/^[1-9]\d*$/.test(String(row[3]))) {
        throw new Error('Registro de marcador inválido.');
      }
      return ['', row[1].trim(), String(row[2]), Number(row[3])];
    });

    const lock = LockService.getScriptLock();
    lock.waitLock(30000);
    try {
      const sheets = SpreadsheetApp.openById(SPREADSHEET_ID).getSheets();
      if (sheets.length === 0) {
        throw new Error('A planilha não contém nenhuma aba.');
      }
      const sheet = sheets[0];
      const firstRow = sheet.getLastRow() + 1;
      sheet.getRange(firstRow, 1, rows.length, 4).setValues(rows);
    } finally {
      lock.releaseLock();
    }

    return respond({ ok: true, added: rows.length });
  } catch (error) {
    return respond({ ok: false, error: error.message });
  }
}

function respond(result) {
  return ContentService.createTextOutput(JSON.stringify(result))
    .setMimeType(ContentService.MimeType.JSON);
}
