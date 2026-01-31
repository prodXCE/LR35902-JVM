package dev.emulator.cartridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Cartridge {
    private byte[] romData;

    public Cartridge(String filePath) throws IOException {
        // Load the entire file into a byte array
        File file = new File(filePath);
        this.romData = Files.readAllBytes(file.toPath());

        System.out.println("Loaded Cartridge: " + file.getName());
        System.out.println("Size: " + romData.length + " bytes");
    }

    public int readByte(int address) {
        // Standard ROM handles 0x0000 - 0x7FFF
        if (address >= 0 && address < romData.length) {
            return romData[address] & 0xFF;
        }
        return 0xFF; // Return 0xFF if out of bounds
    }

    public void writeByte(int address, int value) {
        // For a basic ROM (no Memory Bank Controller), ROM is read-only.
        // Advanced games use this to switch banks (MBC), but we ignore it for now.
    }
}
