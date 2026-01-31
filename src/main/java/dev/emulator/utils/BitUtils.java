package dev.emulator.utils;

/**
 * @author prodXCE
 * 14 Nov, 25 14:05
 * i'm writing this class to handle the
 * the translation bw java's signed (-129 to 127) and
 * gameboy's unsigned byte (0 to 255)
 */
public class BitUtils {

    public static int toUnsignedByte(byte b) {
        return b & 0xFF;
    }

    public static int mergeBytes(byte highByte, byte lowByte) {

        // cleaning the bytes first
        int high = toUnsignedByte(highByte);
        int low = toUnsignedByte(lowByte);

        // shifting high byte 8 bits left, then combine with low byte
        return (high << 8) | low;
    }
}
