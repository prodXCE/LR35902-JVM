package dev.emulator.memory;

import dev.emulator.cartridge.Cartridge;
import dev.emulator.display.Display;
import dev.emulator.hardware.InterruptManager;
import dev.emulator.hardware.Joypad;
import dev.emulator.hardware.Ppu;
import dev.emulator.hardware.Timer;

public class MemoryBus {
    private final byte[] wram = new byte[0x2000]; // 8KB Working RAM
    private final byte[] hram = new byte[0x80];   // 127 Bytes High RAM

    private final Timer timer;
    private final Ppu ppu;
    private final Joypad joypad;
    private final InterruptManager interruptManager;
    private final Display display;
    private Cartridge cartridge;

    // Serial debug buffer
    private int sb = 0;

    public MemoryBus(Display display) {
        this.display = display;
        this.interruptManager = new InterruptManager();
        this.timer = new Timer(interruptManager);
        this.ppu = new Ppu(interruptManager, display);
        this.joypad = new Joypad(interruptManager);
    }

    public void insertCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public Timer getTimer() { return timer; }
    public Joypad getJoypad() { return joypad; }

    public void tick(int cycles) {
        timer.tick(cycles);
        ppu.tick(cycles);
    }

    // --- DMA TRANSFER (The Fix for Sprites) ---
    private void dmaTransfer(int value) {
        // value is the upper byte of the source address (e.g., value=0xC0 means source is 0xC000)
        int source = value * 0x100;

        for (int i = 0; i < 160; i++) { // Copy 160 bytes (40 sprites * 4 bytes)
            int byteValue = readByte(source + i);
            ppu.writeByte(0xFE00 + i, byteValue); // Write directly to OAM
        }
    }

    public int readByte(int address) {
        if (address < 0x8000) {
            return (cartridge != null) ? cartridge.readByte(address) : 0xFF;
        } else if (address < 0xA000) {
            return ppu.readByte(address);
        } else if (address < 0xC000) {
            return 0xFF;
        } else if (address < 0xE000) {
            return wram[address - 0xC000] & 0xFF;
        } else if (address < 0xFE00) {
            return wram[address - 0xE000] & 0xFF;
        } else if (address < 0xFEA0) {
            return ppu.readByte(address); // OAM Read
        } else if (address < 0xFF00) {
            return 0xFF;
        } else if (address < 0xFF80) {
            if (address == 0xFF00) return joypad.readByte();
            if (address >= 0xFF04 && address <= 0xFF07) return timer.readByte(address);
            if (address == 0xFF0F) return interruptManager.getInterruptFlag();
            if (address >= 0xFF40 && address <= 0xFF4B) return ppu.readByte(address);
            return 0xFF;
        } else if (address < 0xFFFF) {
            return hram[address - 0xFF80] & 0xFF;
        } else if (address == 0xFFFF) {
            return interruptManager.getInterruptEnable();
        }
        return 0xFF;
    }

    public void writeByte(int address, int value) {
        if (address < 0x8000) {
            if (cartridge != null) cartridge.writeByte(address, value);
        } else if (address < 0xA000) {
            ppu.writeByte(address, value);
        } else if (address < 0xC000) {
            // External RAM
        } else if (address < 0xE000) {
            wram[address - 0xC000] = (byte) value;
        } else if (address < 0xFE00) {
            wram[address - 0xE000] = (byte) value;
        } else if (address < 0xFEA0) {
            ppu.writeByte(address, value); // OAM Write
        } else if (address < 0xFF80) {
            if (address == 0xFF00) { joypad.writeByte(value); return; }
            if (address == 0xFF01) { sb = value; return; }
            if (address == 0xFF02) {
                if (value == 0x81) System.out.print((char)sb);
                return;
            }
            if (address >= 0xFF04 && address <= 0xFF07) { timer.writeByte(address, value); return; }
            if (address == 0xFF0F) { interruptManager.setInterruptFlag(value); return; }

            // --- DMA TRIGGER ---
            if (address == 0xFF46) { dmaTransfer(value); return; }

            if (address >= 0xFF40 && address <= 0xFF4B) { ppu.writeByte(address, value); return; }
        } else if (address < 0xFFFF) {
            hram[address - 0xFF80] = (byte) value;
        } else if (address == 0xFFFF) {
            interruptManager.setInterruptEnable(value);
        }
    }
}
