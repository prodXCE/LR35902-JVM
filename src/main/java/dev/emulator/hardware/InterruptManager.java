package dev.emulator.hardware;

public class InterruptManager {
    // 0xFFFF: Interrupt Enable (IE) - Which interrupts does the game WANT?
    private int ie = 0;

    // 0xFF0F: Interrupt Flag (IF) - Which interrupts have HAPPENED?
    private int ifReg = 0;

    // Bit 0: V-Blank (0x40)
    // Bit 1: LCD STAT (0x48)
    // Bit 2: Timer    (0x50)
    // Bit 3: Serial   (0x58)
    // Bit 4: Joypad   (0x60)

    public void requestInterrupt(int bit) {
        ifReg |= (1 << bit);
    }

    public int getInterruptEnable() {
        return ie;
    }

    public void setInterruptEnable(int value) {
        this.ie = value;
    }

    public int getInterruptFlag() {
        return ifReg;
    }

    public void setInterruptFlag(int value) {
        this.ifReg = value;
    }
}
