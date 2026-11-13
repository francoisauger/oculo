package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Map;

/**
 * Oculo: QR codes generated on the device (ZXing core) instead of api.qrserver.com, so the sign-in code
 * and shared links never leave the TV. Dark modules on white, 4-module quiet zone, integer pixels per module.
 */
public final class OculoQr {
    /** Quiet zone required by the QR spec, in modules. */
    public static final int QUIET_ZONE_MODULES = 4;
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private OculoQr() {
    }

    /**
     * QR matrix of {@code data} with one bit per module (no scaling), quiet zone included. Pure Java: unit-testable.
     */
    public static BitMatrix encode(String data) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE_MODULES);
        // Size 0: ZXing returns the natural size (one pixel per module, margin included).
        return new QRCodeWriter().encode(data != null ? data : "", BarcodeFormat.QR_CODE, 0, 0, hints);
    }

    /**
     * Crisp bitmap at least {@code minSizePx} wide: each module is an integer block of pixels. Null on failure.
     */
    public static Bitmap toBitmap(String data, int minSizePx) {
        try {
            BitMatrix matrix = encode(data);
            int modules = matrix.getWidth();
            int scale = Math.max(1, (minSizePx + modules - 1) / modules);
            int size = modules * scale;
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                int my = y / scale;
                int row = y * size;
                for (int x = 0; x < size; x++) {
                    pixels[row + x] = matrix.get(x / scale, my) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (WriterException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Self-contained page ({@code data:} URL, nothing fetched) showing the QR of {@code data} centered on screen,
     * for the built-in web view that used to load the api.qrserver.com image.
     */
    public static String toQrPageUrl(String data) {
        Bitmap bitmap = toBitmap(data, 800);
        String img = "";
        if (bitmap != null) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
            bitmap.recycle();
            img = "<img alt='' src='data:image/png;base64," + Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP) + "'>";
        }
        String html = "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'><style>"
                + "html,body{margin:0;height:100%;background:#000}"
                + "body{display:flex;align-items:center;justify-content:center}"
                + "img{height:78vh;width:auto;max-width:90vw;image-rendering:pixelated;image-rendering:crisp-edges}"
                + "</style></head><body>" + img + "</body></html>";
        return "data:text/html;charset=utf-8;base64," + Base64.encodeToString(html.getBytes(UTF_8), Base64.NO_WRAP);
    }
}
