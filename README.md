# LR35902-JVM: Game Boy Emulator

A from-scratch Nintendo Game Boy (DMG) emulator written in pure Java. This project implements the core hardware components of the Game Boy, including the custom LR35902 CPU, Memory Bus, PPU (Pixel Processing Unit), and Hardware Timers, without relying on any external libraries.

**Current Status:** *Work In Progress (Bootable)* It successfully boots Tier-1 games like *Tetris* and *Alleyway*, rendering backgrounds, windows, and sprites with working input.

## Features

* **CPU Core:** Complete implementation of the Sharp LR35902 processor (hybrid Z80/8080) including:
    * All 8-bit and 16-bit loads, arithmetic, and control flow instructions.
    * Prefix `CB` instructions (Bit, Set, Reset, Rotate).
    * Accurate flag handling (Z, N, H, C).
* **Memory Management:** Full memory bus implementation mapping:
    * 32KB ROM (Cartridge)
    * 8KB VRAM (Video RAM)
    * 8KB WRAM (Working RAM)
    * OAM (Object Attribute Memory)
    * HRAM (High RAM) and I/O Registers.
* **PPU (Graphics):**
    * Tile-based background rendering.
    * Window overlay support.
    * Sprite (OBJ) rendering (8x8 and 8x16 modes).
    * DMA Transfer implementation for fast OAM updates.
    * Standard Game Boy palette mapping.
* **Hardware Timer:** Functional `DIV` and `TIMA` registers for random number generation and game timing.
* **Input:** Interrupt-based Joypad implementation mapped to the keyboard.

## 🕹️ Controls

| Game Boy Button | Keyboard Key |
| :--- | :--- |
| **D-Pad Up** | `Arrow Up` |
| **D-Pad Down** | `Arrow Down` |
| **D-Pad Left** | `Arrow Left` |
| **D-Pad Right** | `Arrow Right` |
| **A Button** | `Z` |
| **B Button** | `X` |
| **Start** | `Enter` |
| **Select** | `Shift` |

## 🚀 Setup & Usage

### Prerequisites
* Java Development Kit (JDK) 8 or higher.

### Compilation
Navigate to the project root and compile the source code:

```bash
javac -d out src/main/java/dev/emulator/*.java src/main/java/dev/emulator/*/*.java

## Running the Emulator

1. Place a valid Game Boy ROM file (e.g., `tetris.gb` or `alleyway.gb`) in the project root.

2. Update `src/main/java/dev/emulator/Main.java` to point to your ROM file string if necessary.

3. Run the compiled class:

```bash
java -cp out dev.emulator.Main



## Technical Architecture

The emulator is structured into modular components that mimic the physical Game Boy hardware:

- **`cpu/Cpu.java`**  
  The brain. Handles the Fetch–Decode–Execute cycle. Manages CPU registers and interacts with the `MemoryBus`.

- **`memory/MemoryBus.java`**  
  The nervous system. Routes read/write requests to the correct component (Cartridge, PPU, RAM, or I/O) based on the memory address map.

- **`hardware/Ppu.java`**  
  The graphics card. Reads VRAM/OAM and renders pixels to a frame buffer, handling scanline timing and LCD status modes.

- **`display/Display.java`**  
  The screen. A `JPanel` wrapper that uses `BufferedImage` and `DataBufferInt` for high-performance pixel rendering.

---

## Known Issues & Roadmap

As this is an active learning project, the following limitations exist (To-Do list):

- **Graphics Glitches**  
  Sprite rendering occasionally flickers or misaligns in fast-moving scenes  
  *(likely due to timing synchronization issues between CPU cycles and PPU modes).*

- **Memory Bank Controllers (MBC)**  
  Currently supports only 32KB ROMs (Tetris, Dr. Mario, Alleyway).  
  Larger games (Zelda, Pokémon) requiring MBC1/MBC3 are not yet supported.

- **Audio (APU)**  
  The Audio Processing Unit is not implemented. The emulator currently runs without sound.

- **Timing Accuracy**  
  Frame timing uses a *busy-wait* loop for 60 FPS. Functional, but not strictly cycle-accurate to real hardware specifications.
