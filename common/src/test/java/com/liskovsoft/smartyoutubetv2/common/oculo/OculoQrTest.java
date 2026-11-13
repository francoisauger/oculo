package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OculoQrTest {
    private static final String SIGN_IN_URL = "https://www.youtube.com/tv/activate?user_code=ABC-DEF-GHI";

    @Test
    public void encodedMatrixDecodesBackToTheSameUrl() throws Exception {
        BitMatrix matrix = OculoQr.encode(SIGN_IN_URL);
        assertEquals(matrix.getWidth(), matrix.getHeight());

        Result result = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(toLuminance(matrix, 6))));
        assertEquals(SIGN_IN_URL, result.getText());
    }

    @Test
    public void matrixHasQuietZone() throws Exception {
        BitMatrix matrix = OculoQr.encode(SIGN_IN_URL);
        int size = matrix.getWidth();
        for (int i = 0; i < size; i++) {
            for (int q = 0; q < OculoQr.QUIET_ZONE_MODULES; q++) {
                assertFalse(matrix.get(i, q));
                assertFalse(matrix.get(q, i));
                assertFalse(matrix.get(i, size - 1 - q));
                assertFalse(matrix.get(size - 1 - q, i));
            }
        }
    }

    /** Same pixels OculoQr.toBitmap() draws: dark modules black, the rest white, integer scale. */
    private static LuminanceSource toLuminance(BitMatrix matrix, int scale) {
        int size = matrix.getWidth() * scale;
        byte[] lum = new byte[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                lum[y * size + x] = (byte) (matrix.get(x / scale, y / scale) ? 0 : 255);
            }
        }
        return new LuminanceSource(size, size) {
            @Override
            public byte[] getRow(int y, byte[] row) {
                if (row == null || row.length < size) {
                    row = new byte[size];
                }
                System.arraycopy(lum, y * size, row, 0, size);
                return row;
            }

            @Override
            public byte[] getMatrix() {
                return lum;
            }
        };
    }
}
