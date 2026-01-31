package dev.emulator.cpu;

import dev.emulator.memory.MemoryBus;

public class Cpu {
    private Registers registers;
    private MemoryBus bus;
    public long cycles;

    public Cpu(MemoryBus bus) {
        this.bus = bus;
        this.registers = new Registers();
        this.cycles = 0;
    }

    public Registers getRegisters() { return registers; }

    public void step() {
        long startCycles = this.cycles;

        // 1. Handle Interrupts
        if (handleInterrupts()) {
            bus.tick((int) (this.cycles - startCycles));
            return;
        }

        // 2. Fetch
        int pc = registers.pc;
        int opcode = bus.readByte(pc);
        registers.incrementPC();

        // 3. Decode & Execute
        executeOpcode(opcode);

        // 4. Sync Hardware
        long delta = this.cycles - startCycles;
        bus.tick((int) delta);
    }

    private void executeOpcode(int opcode) {
        // --- BLOCK 1: 8-BIT LOADS (0x40 - 0x7F) ---
        // LD x, y (Load register y into register x)
        if (opcode >= 0x40 && opcode < 0x76) {
            int destIndex = (opcode >> 3) & 7;
            int srcIndex = opcode & 7;
            if (destIndex == 6) { // LD (HL), r
                bus.writeByte(registers.getHL(), getReg(srcIndex));
                cycles += 8;
            } else if (srcIndex == 6) { // LD r, (HL)
                setReg(destIndex, bus.readByte(registers.getHL()));
                cycles += 8;
            } else { // LD r, r
                setReg(destIndex, getReg(srcIndex));
                cycles += 4;
            }
            return;
        }

        // HALT (0x76)
        if (opcode == 0x76) {
            cycles += 4;
            return;
        }

        // Continue 0x77-0x7F (LD (HL), r and LD A, r)
        if (opcode >= 0x77 && opcode < 0x80) {
            int destIndex = (opcode >> 3) & 7;
            int srcIndex = opcode & 7;
            if (destIndex == 6) {
                bus.writeByte(registers.getHL(), getReg(srcIndex));
                cycles += 8;
            } else {
                setReg(destIndex, getReg(srcIndex));
                cycles += 4;
            }
            return;
        }

        // --- BLOCK 2: ALU OPERATIONS (0x80 - 0xBF) ---
        if (opcode >= 0x80 && opcode < 0xC0) {
            int type = (opcode >> 3) & 7; // 0=ADD, 1=ADC, ... 7=CP
            int srcIndex = opcode & 7;
            int val = (srcIndex == 6) ? bus.readByte(registers.getHL()) : getReg(srcIndex);

            alu(type, val);
            cycles += (srcIndex == 6) ? 8 : 4;
            return;
        }

        // --- BLOCK 3: STANDARD INSTRUCTIONS ---
        switch (opcode) {
            case 0x00: cycles += 4; break; // NOP

            // --- 16-BIT LOADS ---
            case 0x01: registers.setBC(readWord()); cycles += 12; break; // LD BC, d16
            case 0x11: registers.setDE(readWord()); cycles += 12; break; // LD DE, d16
            case 0x21: registers.setHL(readWord()); cycles += 12; break; // LD HL, d16
            case 0x31: registers.sp = readWord(); cycles += 12; break;   // LD SP, d16

            // --- 16-BIT INC/DEC ---
            case 0x03: registers.setBC((registers.getBC() + 1) & 0xFFFF); cycles += 8; break;
            case 0x13: registers.setDE((registers.getDE() + 1) & 0xFFFF); cycles += 8; break;
            case 0x23: registers.setHL((registers.getHL() + 1) & 0xFFFF); cycles += 8; break;
            case 0x33: registers.sp = (registers.sp + 1) & 0xFFFF; cycles += 8; break;

            case 0x0B: registers.setBC((registers.getBC() - 1) & 0xFFFF); cycles += 8; break;
            case 0x1B: registers.setDE((registers.getDE() - 1) & 0xFFFF); cycles += 8; break;
            case 0x2B: registers.setHL((registers.getHL() - 1) & 0xFFFF); cycles += 8; break;
            case 0x3B: registers.sp = (registers.sp - 1) & 0xFFFF; cycles += 8; break;

            // --- 16-BIT ARITHMETIC (NEW) ---
            case 0x09: addHL(registers.getBC()); cycles += 8; break; // ADD HL, BC
            case 0x19: addHL(registers.getDE()); cycles += 8; break; // ADD HL, DE
            case 0x29: addHL(registers.getHL()); cycles += 8; break; // ADD HL, HL
            case 0x39: addHL(registers.sp);      cycles += 8; break; // ADD HL, SP

            case 0xE8: // ADD SP, e8
                int offset = readByteSigned();
                int result = registers.sp + offset;
                boolean hStack = ((registers.sp & 0xF) + (offset & 0xF)) > 0xF;
                boolean cStack = ((registers.sp & 0xFF) + (offset & 0xFF)) > 0xFF;
                setFlags(false, false, hStack, cStack);
                registers.sp = result & 0xFFFF;
                cycles += 16;
                break;

            case 0xF8: // LD HL, SP+e8
                int off = readByteSigned();
                int res = registers.sp + off;
                boolean hVal = ((registers.sp & 0xF) + (off & 0xF)) > 0xF;
                boolean cVal = ((registers.sp & 0xFF) + (off & 0xFF)) > 0xFF;
                setFlags(false, false, hVal, cVal);
                registers.setHL(res & 0xFFFF);
                cycles += 12;
                break;

            // --- 8-BIT IMMEDIATE LOADS ---
            case 0x06: registers.b = readByte(); cycles += 8; break;
            case 0x0E: registers.c = readByte(); cycles += 8; break;
            case 0x16: registers.d = readByte(); cycles += 8; break;
            case 0x1E: registers.e = readByte(); cycles += 8; break;
            case 0x26: registers.h = readByte(); cycles += 8; break;
            case 0x2E: registers.l = readByte(); cycles += 8; break;
            case 0x3E: registers.a = readByte(); cycles += 8; break;
            case 0x36: bus.writeByte(registers.getHL(), readByte()); cycles += 12; break; // LD (HL), d8

            // --- MEMORY LOADS ---
            case 0x0A: registers.a = bus.readByte(registers.getBC()); cycles += 8; break; // LD A, (BC)
            case 0x1A: registers.a = bus.readByte(registers.getDE()); cycles += 8; break; // LD A, (DE)
            case 0x02: bus.writeByte(registers.getBC(), registers.a); cycles += 8; break; // LD (BC), A
            case 0x12: bus.writeByte(registers.getDE(), registers.a); cycles += 8; break; // LD (DE), A
            case 0xEA: bus.writeByte(readWord(), registers.a); cycles += 16; break;       // LD (nn), A
            case 0xFA: registers.a = bus.readByte(readWord()); cycles += 16; break;       // LD A, (nn)

            // --- LD (HL+/-) ---
            case 0x22: // LD (HL+), A
                bus.writeByte(registers.getHL(), registers.a);
                registers.setHL((registers.getHL() + 1) & 0xFFFF);
                cycles += 8;
                break;
            case 0x2A: // LD A, (HL+)
                registers.a = bus.readByte(registers.getHL());
                registers.setHL((registers.getHL() + 1) & 0xFFFF);
                cycles += 8;
                break;
            case 0x32: // LD (HL-), A
                bus.writeByte(registers.getHL(), registers.a);
                registers.setHL((registers.getHL() - 1) & 0xFFFF);
                cycles += 8;
                break;
            case 0x3A: // LD A, (HL-)
                registers.a = bus.readByte(registers.getHL());
                registers.setHL((registers.getHL() - 1) & 0xFFFF);
                cycles += 8;
                break;

            // --- IO / HRAM ---
            case 0xE0: bus.writeByte(0xFF00 | readByte(), registers.a); cycles += 12; break; // LDH (n), A
            case 0xF0: registers.a = bus.readByte(0xFF00 | readByte()); cycles += 12; break; // LDH A, (n)
            case 0xE2: bus.writeByte(0xFF00 | registers.c, registers.a); cycles += 8; break; // LD (C), A
            case 0xF2: registers.a = bus.readByte(0xFF00 | registers.c); cycles += 8; break; // LD A, (C)

            // --- ALU IMMEDIATE ---
            case 0xC6: alu(0, readByte()); cycles += 8; break; // ADD A, d8
            case 0xCE: alu(1, readByte()); cycles += 8; break; // ADC A, d8
            case 0xD6: alu(2, readByte()); cycles += 8; break; // SUB d8
            case 0xDE: alu(3, readByte()); cycles += 8; break; // SBC d8
            case 0xE6: alu(4, readByte()); cycles += 8; break; // AND d8
            case 0xEE: alu(5, readByte()); cycles += 8; break; // XOR d8
            case 0xF6: alu(6, readByte()); cycles += 8; break; // OR d8
            case 0xFE: alu(7, readByte()); cycles += 8; break; // CP d8

            // --- INCREMENT / DECREMENT ---
            case 0x04: registers.b = inc(registers.b); cycles += 4; break;
            case 0x05: registers.b = dec(registers.b); cycles += 4; break;
            case 0x0C: registers.c = inc(registers.c); cycles += 4; break;
            case 0x0D: registers.c = dec(registers.c); cycles += 4; break;
            case 0x14: registers.d = inc(registers.d); cycles += 4; break;
            case 0x15: registers.d = dec(registers.d); cycles += 4; break;
            case 0x1C: registers.e = inc(registers.e); cycles += 4; break;
            case 0x1D: registers.e = dec(registers.e); cycles += 4; break;
            case 0x24: registers.h = inc(registers.h); cycles += 4; break;
            case 0x25: registers.h = dec(registers.h); cycles += 4; break;
            case 0x2C: registers.l = inc(registers.l); cycles += 4; break;
            case 0x2D: registers.l = dec(registers.l); cycles += 4; break;
            case 0x3C: registers.a = inc(registers.a); cycles += 4; break;
            case 0x3D: registers.a = dec(registers.a); cycles += 4; break;
            case 0x34: // INC (HL)
                int addrInc = registers.getHL();
                bus.writeByte(addrInc, inc(bus.readByte(addrInc)));
                cycles += 12;
                break;
            case 0x35: // DEC (HL)
                int addrDec = registers.getHL();
                bus.writeByte(addrDec, dec(bus.readByte(addrDec)));
                cycles += 12;
                break;

            // --- JUMPS ---
            case 0xC3: jump(readWord()); cycles += 16; break; // JP nn
            case 0xE9: registers.pc = registers.getHL(); cycles += 4; break; // JP (HL)
            case 0x18: jumpRel(readByteSigned()); cycles += 12; break; // JR n

            case 0x20: if (!getZ()) { jumpRel(readByteSigned()); cycles += 12; } else { registers.incrementPC(); cycles += 8; } break;
            case 0x28: if (getZ())  { jumpRel(readByteSigned()); cycles += 12; } else { registers.incrementPC(); cycles += 8; } break;
            case 0x30: if (!getC()) { jumpRel(readByteSigned()); cycles += 12; } else { registers.incrementPC(); cycles += 8; } break;
            case 0x38: if (getC())  { jumpRel(readByteSigned()); cycles += 12; } else { registers.incrementPC(); cycles += 8; } break;

            case 0xC2: { int addr = readWord(); if (!getZ()) { jump(addr); cycles += 16; } else { cycles += 12; } break; }
            case 0xCA: { int addr = readWord(); if (getZ())  { jump(addr); cycles += 16; } else { cycles += 12; } break; }
            case 0xD2: { int addr = readWord(); if (!getC()) { jump(addr); cycles += 16; } else { cycles += 12; } break; }
            case 0xDA: { int addr = readWord(); if (getC())  { jump(addr); cycles += 16; } else { cycles += 12; } break; }

            // --- CALLS ---
            case 0xCD: call(readWord()); cycles += 24; break; // CALL nn
            case 0xC4: { int addr = readWord(); if (!getZ()) { call(addr); cycles += 24; } else { cycles += 12; } break; }
            case 0xCC: { int addr = readWord(); if (getZ())  { call(addr); cycles += 24; } else { cycles += 12; } break; }
            case 0xD4: { int addr = readWord(); if (!getC()) { call(addr); cycles += 24; } else { cycles += 12; } break; }
            case 0xDC: { int addr = readWord(); if (getC())  { call(addr); cycles += 24; } else { cycles += 12; } break; }

            // --- RETURNS ---
            case 0xC9: ret(); cycles += 16; break; // RET
            case 0xC0: if (!getZ()) { ret(); cycles += 20; } else { cycles += 8; } break;
            case 0xC8: if (getZ())  { ret(); cycles += 20; } else { cycles += 8; } break;
            case 0xD0: if (!getC()) { ret(); cycles += 20; } else { cycles += 8; } break;
            case 0xD8: if (getC())  { ret(); cycles += 20; } else { cycles += 8; } break;
            case 0xD9: registers.setIme(true); ret(); cycles += 16; break; // RETI

            // --- PUSH / POP ---
            case 0xC5: push(registers.getBC()); cycles += 16; break;
            case 0xD5: push(registers.getDE()); cycles += 16; break;
            case 0xE5: push(registers.getHL()); cycles += 16; break;
            case 0xF5: push(registers.getAF()); cycles += 16; break;
            case 0xC1: registers.setBC(pop()); cycles += 12; break;
            case 0xD1: registers.setDE(pop()); cycles += 12; break;
            case 0xE1: registers.setHL(pop()); cycles += 12; break;
            case 0xF1: registers.setAF(pop()); cycles += 12; break;

            // --- RST ---
            case 0xC7: call(0x00); cycles += 16; break;
            case 0xCF: call(0x08); cycles += 16; break;
            case 0xD7: call(0x10); cycles += 16; break;
            case 0xDF: call(0x18); cycles += 16; break;
            case 0xE7: call(0x20); cycles += 16; break;
            case 0xEF: call(0x28); cycles += 16; break;
            case 0xF7: call(0x30); cycles += 16; break;
            case 0xFF: call(0x38); cycles += 16; break;

            // --- MISC ---
            case 0xCB: executeExtendedOpcode(); break;
            case 0xF3: registers.setIme(false); cycles += 4; break; // DI
            case 0xFB: registers.setIme(true); cycles += 4; break;  // EI

            // --- ROTATES & FLIPS (Accumulator) ---
            case 0x07: { // RLCA
                int a = registers.a;
                boolean carry = (a & 0x80) != 0;
                registers.a = ((a << 1) | (carry ? 1 : 0)) & 0xFF;
                setFlags(false, false, false, carry);
                cycles += 4;
                break;
            }
            case 0x0F: { // RRCA
                int a = registers.a;
                boolean carry = (a & 0x01) != 0;
                registers.a = ((a >> 1) | (carry ? 0x80 : 0)) & 0xFF;
                setFlags(false, false, false, carry);
                cycles += 4;
                break;
            }
            case 0x17: { // RLA (Rotate Left through Carry)
                int a = registers.a;
                boolean oldCarry = getC();
                boolean newCarry = (a & 0x80) != 0;
                registers.a = ((a << 1) | (oldCarry ? 1 : 0)) & 0xFF;
                setFlags(false, false, false, newCarry);
                cycles += 4;
                break;
            }
            case 0x1F: { // RRA (Rotate Right through Carry)
                int a = registers.a;
                boolean oldCarry = getC();
                boolean newCarry = (a & 0x01) != 0;
                registers.a = ((a >> 1) | (oldCarry ? 0x80 : 0)) & 0xFF;
                setFlags(false, false, false, newCarry);
                cycles += 4;
                break;
            }

            // --- CARRY / BCD / CPL ---
            case 0x2F: // CPL (Complement A)
                registers.a = (~registers.a) & 0xFF;
                setFlags((registers.f & 0x80) != 0, true, true, getC()); // N=1, H=1
                cycles += 4;
                break;
            case 0x3F: // CCF (Complement Carry Flag)
                setFlags((registers.f & 0x80) != 0, false, false, !getC()); // N=0, H=0, C=!C
                cycles += 4;
                break;
            case 0x37: // SCF (Set Carry Flag)
                setFlags((registers.f & 0x80) != 0, false, false, true); // N=0, H=0, C=1
                cycles += 4;
                break;
            case 0x27: // DAA (Decimal Adjust Accumulator)
                handleDAA();
                cycles += 4;
                break;

            default:
                throw new IllegalStateException(String.format("Unknown Opcode: 0x%02X at 0x%04X", opcode, registers.pc - 1));
        }
    }

    // --- HELPERS ---

    private void addHL(int value) {
        int hl = registers.getHL();
        int result = hl + value;
        // H flag: Carry from bit 11
        boolean h = ((hl & 0x0FFF) + (value & 0x0FFF)) > 0x0FFF;
        // C flag: Carry from bit 15 (Overflow from 16-bit)
        boolean c = result > 0xFFFF;
        // N is Reset. Z is Preserved.
        boolean z = getZ();
        setFlags(z, false, h, c);
        registers.setHL(result & 0xFFFF);
    }

    private void handleDAA() {
        int a = registers.a;
        boolean n = (registers.f & 0x40) != 0;
        boolean h = (registers.f & 0x20) != 0;
        boolean c = (registers.f & 0x10) != 0;

        if (!n) { // Addition
            if (c || a > 0x99) { a += 0x60; c = true; }
            if (h || (a & 0x0F) > 0x09) { a += 0x06; }
        } else { // Subtraction
            if (c) { a -= 0x60; }
            if (h) { a -= 0x06; }
        }

        a &= 0xFF;
        setFlags(a == 0, n, false, c);
        registers.a = a;
    }

    private int getReg(int index) {
        switch (index) {
            case 0: return registers.b;
            case 1: return registers.c;
            case 2: return registers.d;
            case 3: return registers.e;
            case 4: return registers.h;
            case 5: return registers.l;
            case 7: return registers.a;
            default: return 0;
        }
    }

    private void setReg(int index, int val) {
        switch (index) {
            case 0: registers.b = val; break;
            case 1: registers.c = val; break;
            case 2: registers.d = val; break;
            case 3: registers.e = val; break;
            case 4: registers.h = val; break;
            case 5: registers.l = val; break;
            case 7: registers.a = val; break;
        }
    }

    private int readByte() {
        int val = bus.readByte(registers.pc);
        registers.incrementPC();
        return val;
    }

    private int readByteSigned() {
        byte val = (byte) bus.readByte(registers.pc);
        registers.incrementPC();
        return val;
    }

    private int readWord() {
        int low = readByte();
        int high = readByte();
        return (high << 8) | low;
    }

    private void push(int val) {
        registers.sp = (registers.sp - 1) & 0xFFFF;
        bus.writeByte(registers.sp, (val >> 8) & 0xFF);
        registers.sp = (registers.sp - 1) & 0xFFFF;
        bus.writeByte(registers.sp, val & 0xFF);
    }

    private int pop() {
        int low = bus.readByte(registers.sp);
        registers.sp = (registers.sp + 1) & 0xFFFF;
        int high = bus.readByte(registers.sp);
        registers.sp = (registers.sp + 1) & 0xFFFF;
        return (high << 8) | low;
    }

    private void jump(int addr) { registers.pc = addr; }
    private void jumpRel(int offset) { registers.pc = (registers.pc + offset) & 0xFFFF; }
    private void call(int addr) { push(registers.pc); registers.pc = addr; }
    private void ret() { registers.pc = pop(); }

    private int inc(int val) {
        int res = (val + 1) & 0xFF;
        setFlags(res == 0, false, (val & 0xF) == 0xF, getC());
        return res;
    }

    private int dec(int val) {
        int res = (val - 1) & 0xFF;
        setFlags(res == 0, true, (val & 0xF) == 0, getC());
        return res;
    }

    private void alu(int type, int val) {
        int a = registers.a;
        int res = 0;
        boolean z = false, n = false, h = false, c = false;

        switch (type) {
            case 0: // ADD
                res = a + val;
                z = (res & 0xFF) == 0; n = false;
                h = (a & 0xF) + (val & 0xF) > 0xF;
                c = res > 0xFF;
                registers.a = res & 0xFF;
                break;
            case 1: // ADC
                int carry = getC() ? 1 : 0;
                res = a + val + carry;
                z = (res & 0xFF) == 0; n = false;
                h = (a & 0xF) + (val & 0xF) + carry > 0xF;
                c = res > 0xFF;
                registers.a = res & 0xFF;
                break;
            case 2: // SUB
                res = a - val;
                z = (res & 0xFF) == 0; n = true;
                h = (a & 0xF) < (val & 0xF);
                c = a < val;
                registers.a = res & 0xFF;
                break;
            case 3: // SBC
                int carrySub = getC() ? 1 : 0;
                res = a - val - carrySub;
                z = (res & 0xFF) == 0; n = true;
                h = (a & 0xF) < (val & 0xF) + carrySub;
                c = a < val + carrySub;
                registers.a = res & 0xFF;
                break;
            case 4: // AND
                res = a & val;
                z = res == 0; n = false; h = true; c = false;
                registers.a = res;
                break;
            case 5: // XOR
                res = a ^ val;
                z = res == 0; n = false; h = false; c = false;
                registers.a = res;
                break;
            case 6: // OR
                res = a | val;
                z = res == 0; n = false; h = false; c = false;
                registers.a = res;
                break;
            case 7: // CP
                res = a - val;
                z = (res & 0xFF) == 0; n = true;
                h = (a & 0xF) < (val & 0xF);
                c = a < val;
                break;
        }
        setFlags(z, n, h, c);
    }

    private void executeExtendedOpcode() {
        int op = readByte();
        int regIdx = op & 7;
        int type = (op >> 3) & 7;
        int bit = (op >> 3) & 7;

        int val = (regIdx == 6) ? bus.readByte(registers.getHL()) : getReg(regIdx);
        int res = val;

        if (op < 0x40) {
            boolean c = false;
            switch(type) {
                case 0: c = (val & 0x80) != 0; res = ((val << 1) | (c ? 1 : 0)) & 0xFF; break; // RLC
                case 1: c = (val & 1) != 0; res = ((val >> 1) | (c ? 0x80 : 0)) & 0xFF; break; // RRC
                case 2: boolean oldC = getC(); c = (val & 0x80) != 0; res = ((val << 1) | (oldC ? 1 : 0)) & 0xFF; break; // RL
                case 3: boolean oldC2 = getC(); c = (val & 1) != 0; res = ((val >> 1) | (oldC2 ? 0x80 : 0)) & 0xFF; break; // RR
                case 4: c = (val & 0x80) != 0; res = (val << 1) & 0xFF; break; // SLA
                case 5: c = (val & 1) != 0; res = ((val >> 1) | (val & 0x80)) & 0xFF; break; // SRA
                case 6: res = ((val & 0xF) << 4) | ((val & 0xF0) >> 4); c = false; break; // SWAP
                case 7: c = (val & 1) != 0; res = (val >> 1) & 0xFF; break; // SRL
            }
            setFlags(res == 0, false, false, c);
            if (regIdx == 6) { bus.writeByte(registers.getHL(), res); cycles += 16; }
            else { setReg(regIdx, res); cycles += 8; }
        }
        else if (op < 0x80) { // BIT
            boolean z = (val & (1 << bit)) == 0;
            setFlags(z, false, true, getC());
            cycles += (regIdx == 6) ? 12 : 8;
        }
        else if (op < 0xC0) { // RES
            bit = (op >> 3) & 7;
            res = val & ~(1 << bit);
            if (regIdx == 6) { bus.writeByte(registers.getHL(), res); cycles += 16; }
            else { setReg(regIdx, res); cycles += 8; }
        }
        else { // SET
            bit = (op >> 3) & 7;
            res = val | (1 << bit);
            if (regIdx == 6) { bus.writeByte(registers.getHL(), res); cycles += 16; }
            else { setReg(regIdx, res); cycles += 8; }
        }
    }

    private void setFlags(boolean z, boolean n, boolean h, boolean c) {
        int flags = 0;
        if (z) flags |= 0x80;
        if (n) flags |= 0x40;
        if (h) flags |= 0x20;
        if (c) flags |= 0x10;
        registers.f = flags;
    }

    private boolean getZ() { return (registers.f & 0x80) != 0; }
    private boolean getC() { return (registers.f & 0x10) != 0; }

    private boolean handleInterrupts() {
        if (!registers.isIme()) return false;
        int ie = bus.readByte(0xFFFF);
        int ifReg = bus.readByte(0xFF0F);
        int fired = ie & ifReg & 0x1F;
        if (fired != 0) {
            registers.setIme(false);
            push(registers.pc);
            int vector = 0;
            if ((fired & 0x01) != 0) { vector = 0x40; bus.writeByte(0xFF0F, ifReg & ~0x01); }
            else if ((fired & 0x02) != 0) { vector = 0x48; bus.writeByte(0xFF0F, ifReg & ~0x02); }
            else if ((fired & 0x04) != 0) { vector = 0x50; bus.writeByte(0xFF0F, ifReg & ~0x04); }
            else if ((fired & 0x08) != 0) { vector = 0x58; bus.writeByte(0xFF0F, ifReg & ~0x08); }
            else if ((fired & 0x10) != 0) { vector = 0x60; bus.writeByte(0xFF0F, ifReg & ~0x10); }
            registers.pc = vector;
            cycles += 20;
            return true;
        }
        return false;
    }
}
