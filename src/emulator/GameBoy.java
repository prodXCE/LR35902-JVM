package emulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This is the motherboard
 * this holds the components (cpu, mmu) and manages the main exec
 */
public class GameBoy {

    private final Mmu mmu;
    private final Cpu cpu;

    public GameBoy() {
        this.mmu = new Mmu();
        this.cpu = new Cpu(mmu);
        System.out.println("Game Boy initialized.");
    }

    /**
     * Loads a ROM file and resets the system to a ready-to-play state.
     */
    public void loadRom(String filePath) throws IOException {
        byte[] ramData = Files.readAllBytes(Path.of(filePath));

        // loading the binary data into Mmu
        mmu.loadRom(romData)

        // reset the hardware
        mmu.reset();

        // reset the Cpu
        cpu.reset();

        System.out.println("ROM loaded and system reset. PC set to 0x0100");
    }

    /**
     * runs the emulator for one step
     * @return M-Cycles taken.
     */
    public int tick() {

        // cpu step
        int cycles = cpu.step();

        // timer sync
        mmu.getTimer().tick(cycles);

        // ppu sync
        mmu.getPpu().tick(cycles);

        return cycles;
    }

    public Cpu getCpu() { return cpu; }
    public Mmu getMmu() { return mmu; }
    public Joypad getJoypad() { return mmu.getJoypad(); }

}
