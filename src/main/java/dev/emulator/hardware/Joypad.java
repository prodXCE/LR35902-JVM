package dev.emulator.hardware;

public class Joypad {
    private final InterruptManager interruptManager;

    // 0xFF00 Register State
    // Bit 5 = Select Action Buttons (A, B, Start, Select)
    // Bit 4 = Select Direction Buttons (Right, Left, Up, Down)
    private int p1 = 0xFF;

    // Current Button States (0 = Pressed, 1 = Released)
    // We store them as standard variables for easy updating
    private int buttons = 0x0F;     // Start, Select, B, A (Lower 4 bits)
    private int directions = 0x0F;  // Down, Up, Left, Right (Lower 4 bits)

    public Joypad(InterruptManager interruptManager) {
        this.interruptManager = interruptManager;
    }

    // Called by the MemoryBus when the game reads 0xFF00
    public int readByte() {
        // Start with high nibble (which is read/write)
        int result = p1 & 0xF0;

        // If Bit 4 is 0, Game wants Directions
        if ((p1 & 0x10) == 0) {
            result |= (directions & 0x0F);
        }
        // If Bit 5 is 0, Game wants Buttons
        else if ((p1 & 0x20) == 0) {
            result |= (buttons & 0x0F);
        }
        // Otherwise return nothing pressed (0xF)
        else {
            result |= 0x0F;
        }

        return result;
    }

    // Called by MemoryBus when game writes to 0xFF00
    public void writeByte(int value) {
        // Only bits 4 and 5 are writable
        p1 = (p1 & 0xCF) | (value & 0x30);
    }

    // --- INTERFACE FOR KEYBOARD (Called by Display) ---

    // Bit 0: Right / A
    // Bit 1: Left / B
    // Bit 2: Up / Select
    // Bit 3: Down / Start

    public void keyPressed(int key) {
        boolean wasUnpressed = true; // Simplified interrupt logic check

        // Directions
        if (key == 0) directions &= ~0x01; // Right
        if (key == 1) directions &= ~0x02; // Left
        if (key == 2) directions &= ~0x04; // Up
        if (key == 3) directions &= ~0x08; // Down

        // Buttons
        if (key == 4) buttons &= ~0x01; // A
        if (key == 5) buttons &= ~0x02; // B
        if (key == 6) buttons &= ~0x04; // Select
        if (key == 7) buttons &= ~0x08; // Start

        interruptManager.requestInterrupt(4); // Request Joypad Interrupt
    }

    public void keyReleased(int key) {
        if (key == 0) directions |= 0x01;
        if (key == 1) directions |= 0x02;
        if (key == 2) directions |= 0x04;
        if (key == 3) directions |= 0x08;

        if (key == 4) buttons |= 0x01;
        if (key == 5) buttons |= 0x02;
        if (key == 6) buttons |= 0x04;
        if (key == 7) buttons |= 0x08;
    }
}
