package dev.emulator;

import dev.emulator.cartridge.Cartridge;
import dev.emulator.cpu.Cpu;
import dev.emulator.display.Display;
import dev.emulator.memory.MemoryBus;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String romPath = "alleyway.gb"; // Set to "tetris.gb" or "alleyway.gb"

        try {
            // 1. Initialize Hardware
            Cartridge cart = new Cartridge(romPath);
            Display display = new Display();
            MemoryBus bus = new MemoryBus(display);

            // 2. Link Joypad
            display.setJoypad(bus.getJoypad());

            // 3. Insert Cartridge & Start CPU
            bus.insertCartridge(cart);
            Cpu cpu = new Cpu(bus);

            System.out.println("Emulator Started: " + romPath);

            // --- TIMING CONSTANTS ---
            // Game Boy Clock: 4,194,304 Hz
            // Screen Refresh: 59.7 FPS (~60)
            // Cycles per Frame: 70224
            long cyclesPerFrame = 70224;
            long nextFrameCycleCount = cyclesPerFrame;

            long lastFrameTime = System.nanoTime();
            long targetFrameDuration = 1_000_000_000 / 60; // ~16,666,666 ns (16.6ms)

            // --- GAME LOOP ---
            while (true) {
                // Run one CPU instruction
                cpu.step();

                // Check if we have processed enough cycles for one frame
                if (cpu.cycles >= nextFrameCycleCount) {
                    nextFrameCycleCount += cyclesPerFrame;

                    // SYNC TO 60 FPS
                    long now = System.nanoTime();
                    while (now - lastFrameTime < targetFrameDuration) {
                        now = System.nanoTime(); // Busy-wait for precision
                        // Thread.yield(); // Optional: Uncomment to lower CPU usage slightly
                    }
                    lastFrameTime = now;
                }
            }

        } catch (IOException e) {
            System.err.println("ROM not found: " + romPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
