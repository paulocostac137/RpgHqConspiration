package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GoogleDriveUtilsTest {

    @Test
    public void extraiIdDeLinkArquivo() {
        assertEquals(
                "abc_123-X",
                GoogleDriveUtils.extrairId(
                        "https://drive.google.com/file/d/abc_123-X/view"
                )
        );
    }

    @Test
    public void extraiIdDeLinkPorParametro() {
        assertEquals(
                "xyz789",
                GoogleDriveUtils.extrairId(
                        "https://drive.google.com/uc?id=xyz789&export=download"
                )
        );
    }

    @Test
    public void rejeitaLinksInvalidos() {
        assertNull(GoogleDriveUtils.extrairId(null));
        assertNull(GoogleDriveUtils.extrairId(""));
        assertNull(GoogleDriveUtils.extrairId("https://example.com/gif.gif"));
        assertNull(GoogleDriveUtils.extrairId("https://example.com/file/d/invalido!"));
    }
}
