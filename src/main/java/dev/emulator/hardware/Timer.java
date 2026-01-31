package dev.emulator.hardware;

public class Timer {
    private final InterruptManager interruptManager;

    // DIV register (0xFF04): Increments every 256 cycles
    private int divCounter = 0;
    private int div = 0;

    // TIMA (0xFF05): Timer Counter
    private int tima = 0;
    private int timaCounter = 0;

    // TMA (0xFF06): Timer Modulo (Reset value for TIMA)
    private int tma = 0;

    // TAC (0xFF07): Timer Control
    private int tac = 0;

    public Timer(InterruptManager interruptManager) {
        this.interruptManager = interruptManager;
    }

    public void tick(int cycles) {
        // 1. Handle DIV Register (Always increments)
        divCounter += cycles;
        while (divCounter >= 256) {
            div = (div + 1) & 0xFF;
            divCounter -= 256;
        }

        // 2. Handle TIMA (Only if enabled in TAC bit 2)
        if ((tac & 0x04) != 0) {
            timaCounter += cycles;
            int threshold = getThreshold();
            while (timaCounter >= threshold) {
                timaCounter -= threshold;
                tima++;
                if (tima > 0xFF) {
                    tima = tma; // Reset to TMA
                    interruptManager.requestInterrupt(2); // Request Timer Interrupt
                }
            }
        }
    }

    private int getThreshold() {
        switch (tac & 0x03) {
            case 0: return 1024; // 4096 Hz
            case 1: return 16;   // 262144 Hz
            case 2: return 64;   // 65536 Hz
            case 3: return 256;  // 16384 Hz
            default: return 1024;
        }
    }

    public int readByte(int address) {
        switch (address) {
            case 0xFF04: return div;
            case 0xFF05: return tima;
            case 0xFF06: return tma;
            case 0xFF07: return tac;
            default: return 0xFF;
        }
    }

    public void writeByte(int address, int value) {
        switch (address) {
            case 0xFF04: div = 0; break; // Writing to DIV resets it to 0
            case 0xFF05: tima = value; break;
            case 0xFF06: tma = value; break;
            case 0xFF07: tac = value; break;
        }
    }
}
