const SPREADSHEET_ID = '13ESbous-qqBsQf6VU_6Rw4zb_aoBguosGXY9eh86P9M';
const API_TOKEN = '9718';
const COLUMN_HEADERS = ['Link', 'Express', 'Go1', 'On1', 'Out1'];
const ID_HEADER = 'Pov000';

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

    const records = body.rows.map(function (row) {
      if (!Array.isArray(row) || row.length !== 5 ||
          typeof row[0] !== 'string' || row[0].trim() === '' ||
          typeof row[1] !== 'string' || row[1].length > 100 ||
          !isValidFrameList(row[2]) ||
          !isValidFrameList(row[3]) ||
          !isValidFrameList(row[4]) ||
          [row[2], row[3], row[4]].every(function (frames) {
            return String(frames).trim() === '';
          })) {
        throw new Error('Registro de marcador inválido.');
      }
      return [
        row[0].trim(),
        row[1],
        String(row[2]).trim(),
        String(row[3]).trim(),
        String(row[4]).trim()
      ];
    });

    const lock = LockService.getScriptLock();
    lock.waitLock(30000);
    try {
      const sheets = SpreadsheetApp.openById(SPREADSHEET_ID).getSheets();
      if (sheets.length === 0) {
        throw new Error('A planilha não contém nenhuma aba.');
      }
      const sheet = sheets[0];
      ensureIdColumn(sheet);
      const lastColumn = sheet.getLastColumn();
      if (lastColumn === 0) {
        throw new Error('A primeira linha precisa conter os cabeçalhos: ' +
          COLUMN_HEADERS.join(', ') + '.');
      }
      const headerValues = sheet.getRange(1, 1, 1, lastColumn)
        .getDisplayValues()[0];
      const headerMap = {};
      headerValues.forEach(function (header, index) {
        const key = String(header).trim().toLowerCase();
        if (key && headerMap[key] === undefined) {
          headerMap[key] = index + 1;
        }
      });
      const targetColumns = COLUMN_HEADERS.map(function (header) {
        const column = headerMap[header.toLowerCase()];
        if (column === undefined) {
          throw new Error('Cabeçalho não encontrado na planilha: ' + header);
        }
        return column;
      });

      const lastRow = sheet.getLastRow();
      const nextId = preencherIdsExistentes(
        sheet,
        lastRow,
        targetColumns[0]
      );
      const firstRow = lastRow + 1;
      const ids = records.map(function (_, index) {
        return [formatarId(nextId + index)];
      });
      sheet.getRange(firstRow, 1, ids.length, 1).setValues(ids);
      targetColumns.forEach(function (column, index) {
        const values = records.map(function (record) {
          return [record[index]];
        });
        sheet.getRange(firstRow, column, records.length, 1)
          .setValues(values);
      });
    } finally {
      lock.releaseLock();
    }

    return respond({ ok: true, added: records.length });
  } catch (error) {
    return respond({ ok: false, error: error.message });
  }
}

function ensureIdColumn(sheet) {
  const primeiroCabecalho = String(sheet.getRange(1, 1).getDisplayValue())
    .trim();
  if (primeiroCabecalho.toLowerCase() === ID_HEADER.toLowerCase()) {
    return;
  }

  if (primeiroCabecalho !== '') {
    sheet.insertColumnBefore(1);
  }
  sheet.getRange(1, 1).setValue(ID_HEADER);
}

function preencherIdsExistentes(sheet, lastRow, linkColumn) {
  if (lastRow < 2) {
    return 1;
  }

  const rowCount = lastRow - 1;
  const ids = sheet.getRange(2, 1, rowCount, 1).getDisplayValues();
  const links = sheet.getRange(2, linkColumn, rowCount, 1).getDisplayValues();
  let maiorNumero = 0;
  const linhasSemId = [];

  ids.forEach(function (row, index) {
    const id = String(row[0]).trim();
    const link = String(links[index][0]).trim();
    if (id !== '') {
      const match = /^Pov(\d+)$/i.exec(id);
      if (!match) {
        throw new Error('ID inválido na coluna ' + ID_HEADER +
          ', linha ' + (index + 2) + '.');
      }
      maiorNumero = Math.max(maiorNumero, Number(match[1]));
    } else if (link !== '') {
      linhasSemId.push(index);
    }
  });

  linhasSemId.forEach(function (index) {
    maiorNumero += 1;
    ids[index][0] = formatarId(maiorNumero);
  });

  if (linhasSemId.length > 0) {
    sheet.getRange(2, 1, rowCount, 1).setValues(ids);
  }
  return maiorNumero + 1;
}

function formatarId(numero) {
  const numeroTexto = String(numero);
  const largura = Math.max(3, numeroTexto.length);
  return 'Pov' + ('000' + numeroTexto).slice(-largura);
}

function isValidFrameList(value) {
  const frames = String(value).trim();
  return frames === '' ||
    frames.split(',').every(function (frame) {
      return /^[1-9]\d*$/.test(frame.trim());
    });
}

function respond(result) {
  return ContentService.createTextOutput(JSON.stringify(result))
    .setMimeType(ContentService.MimeType.JSON);
}
