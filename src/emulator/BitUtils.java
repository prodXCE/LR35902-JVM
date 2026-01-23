package emulator;

/**
 * Utility class for bit manipulation operations.
 * This class provides methods to handle unsigned conversions,
 * bit checking, and 16-bit word composition.
 */

public class BitUtils {
    private BitUtils() {

    }

    /**
     * Checks if a specific bit is set (1) in a byte.
     * * @param value The byte value.
     * @param position The bit position (0-7)
     * @return true if the bit is 1, false otheriwse.
     */
    public static boolean getBit(int value, int position) {
        return ((value >> position) & 1) == 1;
    }

    /**
     * Sets a specific bit to 1.
     */
    public static int setBit(int value, int position) {
        return value | (1 << position);
    }

    /**
     * Clears a specific bit to 0
     */
    public static int clearBit(int value, int position) {
        return value & -(1 << position);
    }

    /**
     * Combines two bytes into a 16-bit integer (Word)
     * Game Boy being Little Endian, i'm using this as:
     * mergeBytes(memory[i+1], memory[i])
     * * @param highByte The high byte (MSB)
     * @param lowByte The low byte (LSB)
     * @return The combined 16-bit value (0x0000 to )
     */

    public static int mergeBytes(byte highByte, byte lowByte) {
        return ((highByte & 0xFF) << 8) | (lowByte & 0xFF);
    }

    /**
     * Extracts the High Byte (MSB) from a 16-bit integer.
    */
    public static int getHighByte(int word) {
        return (word >> 8) & 0xFF;
    }

    /**
     * Safe check to ensure we are correctly interpreting a Java
     * byte as an unsigned hex string. Useful for debugging.
    */
    public static String toHex(int value) {
        return String.format("0x%02X", value);
    }

    /**
     * Safe check for 16-bit word hex string.
    */
    public static String toHex16(int value) {
        return String.format("0x%04X", value);
    }



}
