package dev.emulator.cpu;

public class Registers {
    // 8-bit Registers (Public for direct access by CPU)
    public int a, b, c, d, e, h, l, f;

    // 16-bit Registers
    public int pc;
    public int sp;

    // Interrupt Master Enable
    private boolean ime;

    public Registers() {
        // Standard Game Boy Power-on values
        this.pc = 0x0100; // Start at entry point (after bootrom)
        this.sp = 0xFFFE;
        this.a = 0x01;
        this.f = 0xB0;
        this.b = 0x00;
        this.c = 0x13;
        this.d = 0x00;
        this.e = 0xD8;
        this.h = 0x01;
        this.l = 0x4D;
        this.ime = false;
    }

    // --- 16-Bit Virtual Registers (Combine 8-bit ones) ---

    public int getAF() {
        return (a << 8) | f;
    }

    public void setAF(int val) {
        a = (val >> 8) & 0xFF;
        f = val & 0xF0; // Lower 4 bits of F are always 0
    }

    public int getBC() {
        return (b << 8) | c;
    }

    public void setBC(int val) {
        b = (val >> 8) & 0xFF;
        c = val & 0xFF;
    }

    public int getDE() {
        return (d << 8) | e;
    }

    public void setDE(int val) {
        d = (val >> 8) & 0xFF;
        e = val & 0xFF;
    }

    public int getHL() {
        return (h << 8) | l;
    }

    public void setHL(int val) {
        h = (val >> 8) & 0xFF;
        l = val & 0xFF;
    }

    // --- PC / SP Helpers ---

    public int getPC() { return pc; }
    public void setPC(int val) { pc = val & 0xFFFF; }
    public void incrementPC() { pc = (pc + 1) & 0xFFFF; }
    public void addPC(int offset) { pc = (pc + offset) & 0xFFFF; }

    public int getSP() { return sp; }
    public void setSP(int val) { sp = val & 0xFFFF; }

    // --- Interrupt Flag ---

    public boolean isIme() { return ime; }
    public void setIme(boolean ime) { this.ime = ime; }
}
