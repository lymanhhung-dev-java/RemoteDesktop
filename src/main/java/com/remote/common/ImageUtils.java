package com.remote.common;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ImageUtils {

    // Nén mảng pixel (int[]) thành byte[] dùng Zlib (Zip)
    // Nhanh hơn JPEG nhiều với giao diện desktop
    public static byte[] compressRaw(int[] pixels, int width, int height) {
        try {
            // 1. Chuyển int[] (4 byte) thành byte[] (raw bytes)
            byte[] rawBytes = new byte[width * height * 4];
            int idx = 0;
            for (int pixel : pixels) {
                // Tách màu: Alpha, Red, Green, Blue
                rawBytes[idx++] = (byte) ((pixel >> 24) & 0xFF);
                rawBytes[idx++] = (byte) ((pixel >> 16) & 0xFF);
                rawBytes[idx++] = (byte) ((pixel >> 8) & 0xFF);
                rawBytes[idx++] = (byte) (pixel & 0xFF);
            }

            // 2. Nén Zlib
            Deflater deflater = new Deflater();
            deflater.setLevel(Deflater.BEST_SPEED); // Ưu tiên tốc độ
            deflater.setInput(rawBytes);
            deflater.finish();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(rawBytes.length);
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                baos.write(buffer, 0, count);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    // Giải nén Zlib ra lại mảng int[]
    public static int[] decompressRaw(byte[] compressedData, int width, int height) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(compressedData);

            byte[] rawBytes = new byte[width * height * 4];
            inflater.inflate(rawBytes);
            inflater.end();

            int[] pixels = new int[width * height];
            int idx = 0;
            for (int i = 0; i < pixels.length; i++) {
                int a = rawBytes[idx++] & 0xFF;
                int r = rawBytes[idx++] & 0xFF;
                int g = rawBytes[idx++] & 0xFF;
                int b = rawBytes[idx++] & 0xFF;
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            return pixels;
        } catch (DataFormatException e) {
            e.printStackTrace();
            return new int[0];
        }
    }
    
    // Giữ lại hàm cũ nếu cần, nhưng bài này ta không dùng đến
    public static byte[] compress(BufferedImage image, float quality) { return null; }
    public static BufferedImage decompress(byte[] data) { return null; }
}