const SPREADSHEET_ID = '1pJ7XtHx0DLSCjsGj0zlJycm-nzqa7a8flnLfoG7k2ks';
const API_TOKEN = '9718';

function doPost(event) {
  try {
    const body = JSON.parse(event.postData.contents);
    if (body.token !== API_TOKEN) {
      return respond({ ok: false, error: 'Token inválido.' });
    }
    if (!Array.isArray(body.rows) || body.rows.length === 0) {
      return respond({ ok: false, error: 'Nenhuma linha recebida.' });
    }

    const rows = body.rows.map(function (row) {
      if (!Array.isArray(row) || row.length !== 4 ||
          row[0] !== '' ||
          typeof row[1] !== 'string' || row[1].trim() === '' ||
          !['G', 'O', 'T'].includes(row[2]) ||
          !/^[1-9]\d*$/.test(String(row[3]))) {
        throw new Error('Registro de marcador inválido.');
      }
      return ['', row[1].trim(), row[2], Number(row[3])];
    });

    const lock = LockService.getScriptLock();
    lock.waitLock(30000);
    try {
      const sheet = SpreadsheetApp.openById(SPREADSHEET_ID)
        .getSheets()[0];
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
