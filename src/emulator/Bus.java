package emulator;

/**
 * This is the system bus.
 * In hardware, this represents the physical wires connecting the CPU
 * to RAM, PPU and Cartridge.
 * In my emulator, it acts as the central interface for memory access.
*/

public interface Bus {

    /**
     * Reads a byte from specified address.
     * @param address The 16bit address (0x0000 - 0xFFFF)
     * @return The 8-bit value (0x00 - 0xFF) as an integer
     */
    int readByte(int address);

    /**
     * Writes a byte to a specified address.
     * * @param address The 16-bit address (0x0000 - 0xFFFF)
     * @param value The 8-bit value (0x00 - 0xFF)
     */
    void writeByte(int address, int value);


}
