package dev.emulator.hardware;

import dev.emulator.display.Display;

public class Ppu {
    private final InterruptManager interruptManager;
    private final Display display;

    // Memory
    private final byte[] vram = new byte[0x2000];
    private final byte[] oam = new byte[0xA0];

    // Local Frame Buffer (160 * 144 pixels)
    private final int[] frameBuffer = new int[160 * 144];

    // Registers
    private int lcdc = 0x91;
    private int stat = 0;
    private int scy = 0;
    private int scx = 0;
    private int ly = 0;
    private int lyc = 0;
    private int bgp = 0xFC;
    private int obp0 = 0xFF;
    private int obp1 = 0xFF;
    private int wy = 0;
    private int wx = 0;

    private int scanlineCounter = 0;

    public Ppu(InterruptManager interruptManager, Display display) {
        this.interruptManager = interruptManager;
        this.display = display;
    }

    public void tick(int cycles) {
        if ((lcdc & 0x80) == 0) {
            ly = 0;
            stat &= 0xFC;
            return;
        }

        scanlineCounter += cycles;

        while (scanlineCounter >= 456) {
            scanlineCounter -= 456;
            ly++;

            if (ly == 144) {
                interruptManager.requestInterrupt(0);
                // SEND BUFFER TO DISPLAY ONCE PER FRAME
                display.refreshFrame(frameBuffer);
            }
            else if (ly > 153) {
                ly = 0;
            }
            else if (ly < 144) {
                drawScanline();
            }
        }
    }

    private void drawScanline() {
        if ((lcdc & 0x01) != 0) renderBackground();
        if ((lcdc & 0x20) != 0) renderWindow();
        if ((lcdc & 0x02) != 0) renderSprites();
    }

    // Helper to write to buffer instead of calling display.setPixel
    private void setPixel(int x, int y, int color) {
        int index = y * 160 + x;
        if (index >= 0 && index < frameBuffer.length) {
            // Convert Game Boy Color ID to Real RGB Color
            frameBuffer[index] = getColorRGB(color);
        }
    }

    private int getColorRGB(int colorId) {
        switch (colorId) {
            case 0: return 0xFFFFFFFF; // White
            case 1: return 0xFFC0C0C0; // Light Gray
            case 2: return 0xFF606060; // Dark Gray
            case 3: return 0xFF000000; // Black
            default: return 0xFFFFFFFF;
        }
    }

    private void renderBackground() {
        int yPos = ly + scy;
        int mapOffset = ((lcdc & 0x08) != 0) ? 0x1C00 : 0x1800;
        int tileRow = (yPos / 8) * 32;

        for (int x = 0; x < 160; x++) {
            int xPos = x + scx;
            int tileCol = (xPos / 8) & 0x1F;
            int tileNum = vram[mapOffset + tileRow + tileCol] & 0xFF;
            int colorId = getPixelColorID(tileNum, xPos % 8, yPos % 8, (lcdc & 0x10) != 0);

            // Apply Palette
            int finalColor = getPaletteColor(colorId, bgp);
            setPixel(x, ly, finalColor);
        }
    }

    private void renderWindow() {
        int windowX = wx - 7;
        if (ly < wy || windowX >= 160) return;

        int mapOffset = ((lcdc & 0x40) != 0) ? 0x1C00 : 0x1800;
        int yPos = ly - wy;
        int tileRow = (yPos / 8) * 32;

        for (int x = 0; x < 160; x++) {
            if (x < windowX) continue;
            int xPos = x - windowX;
            int tileCol = (xPos / 8) & 0x1F;
            int tileNum = vram[mapOffset + tileRow + tileCol] & 0xFF;
            int colorId = getPixelColorID(tileNum, xPos % 8, yPos % 8, (lcdc & 0x10) != 0);

            int finalColor = getPaletteColor(colorId, bgp);
            setPixel(x, ly, finalColor);
        }
    }

    private void renderSprites() {
        boolean use8x16 = (lcdc & 0x04) != 0;
        for (int i = 0; i < 40; i++) {
            int index = i * 4;
            int yPos = (oam[index] & 0xFF) - 16;
            int xPos = (oam[index + 1] & 0xFF) - 8;
            int tileLocation = oam[index + 2] & 0xFF;
            int attributes = oam[index + 3] & 0xFF;

            int height = use8x16 ? 16 : 8;
            if (ly >= yPos && ly < (yPos + height)) {
                int line = ly - yPos;
                if ((attributes & 0x40) != 0) line = height - 1 - line;
                if (use8x16) tileLocation &= 0xFE;

                int data1 = vram[(tileLocation * 16) + (line * 2)] & 0xFF;
                int data2 = vram[(tileLocation * 16) + (line * 2) + 1] & 0xFF;

                for (int tilePixel = 7; tilePixel >= 0; tilePixel--) {
                    int colorBit = tilePixel;
                    if ((attributes & 0x20) != 0) colorBit = 7 - colorBit;

                    int colorLow = (data1 >> colorBit) & 1;
                    int colorHigh = (data2 >> colorBit) & 1;
                    int col = (colorHigh << 1) | colorLow;

                    if (col == 0) continue; // Transparent

                    int x = xPos + (7 - tilePixel);
                    if (x < 0 || x >= 160) continue;

                    int palette = ((attributes & 0x10) != 0) ? obp1 : obp0;
                    int finalColor = getPaletteColor(col, palette);
                    setPixel(x, ly, finalColor);
                }
            }
        }
    }

    private int getPixelColorID(int tileNum, int xBit, int yLine, boolean unsignedMode) {
        int tileDataLocation;
        if (unsignedMode) {
            tileDataLocation = (tileNum * 16);
        } else {
            int signedTileNum = (byte) tileNum;
            tileDataLocation = 0x1000 + (signedTileNum * 16);
        }
        int data1 = vram[tileDataLocation + (yLine * 2)] & 0xFF;
        int data2 = vram[tileDataLocation + (yLine * 2) + 1] & 0xFF;
        int bit = 7 - xBit;
        return ((data2 >> bit) & 1) << 1 | ((data1 >> bit) & 1);
    }

    private int getPaletteColor(int colorId, int paletteAddress) {
        return (paletteAddress >> (colorId * 2)) & 0x03;
    }

    // Bus Interface
    public int readByte(int address) {
        if (address >= 0x8000 && address < 0xA000) return vram[address - 0x8000] & 0xFF;
        if (address >= 0xFE00 && address < 0xFEA0) return oam[address - 0xFE00] & 0xFF;
        switch (address) {
            case 0xFF40: return lcdc;
            case 0xFF41: return stat;
            case 0xFF42: return scy;
            case 0xFF43: return scx;
            case 0xFF44: return ly;
            case 0xFF45: return lyc;
            case 0xFF47: return bgp;
            case 0xFF48: return obp0;
            case 0xFF49: return obp1;
            case 0xFF4A: return wy;
            case 0xFF4B: return wx;
            default: return 0xFF;
        }
    }

    public void writeByte(int address, int value) {
        if (address >= 0x8000 && address < 0xA000) { vram[address - 0x8000] = (byte) value; return; }
        if (address >= 0xFE00 && address < 0xFEA0) { oam[address - 0xFE00] = (byte) value; return; }
        switch (address) {
            case 0xFF40: lcdc = value; break;
            case 0xFF41: stat = value; break;
            case 0xFF42: scy = value; break;
            case 0xFF43: scx = value; break;
            case 0xFF44: ly = 0; break;
            case 0xFF45: lyc = value; break;
            case 0xFF47: bgp = value; break;
            case 0xFF48: obp0 = value; break;
            case 0xFF49: obp1 = value; break;
            case 0xFF4A: wy = value; break;
            case 0xFF4B: wx = value; break;
        }
    }
}
