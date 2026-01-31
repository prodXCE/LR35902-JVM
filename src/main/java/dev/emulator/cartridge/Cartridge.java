package dev.emulator.cartridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Cartridge {
    private byte[] romData;

    public Cartridge(String filePath) throws IOException {
        File file = new File(filePath);
        this.romData = Files.readAllBytes(file.toPath());

        System.out.println("Loaded Cartridge: " + file.getName());
        System.out.println("Size: " + romData.length + " bytes");
    }

    public int readByte(int address) {
        if (address >= 0 && address < romData.length) {
            return romData[address] & 0xFF;
        }
        return 0xFF;
    }

    public void writeByte(int address, int value) {
    }
}
