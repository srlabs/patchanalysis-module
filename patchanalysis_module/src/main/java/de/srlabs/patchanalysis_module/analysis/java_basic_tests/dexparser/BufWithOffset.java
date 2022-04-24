package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.util.Arrays;

public class BufWithOffset {

    public byte[] buf;
    public int offset;

    public BufWithOffset(BufWithOffset bufRef, int offset) {
        this.buf = bufRef.buf;
        this.offset = bufRef.offset + offset;
    }

    public BufWithOffset(byte[] buf, int offset) {
        this.buf = buf;
        this.offset = offset;
    }

    public BufWithOffset(BufWithOffset buf) {
        this(buf, 0);
    }

    public BufWithOffset(byte[] buf) {
        this(buf, 0);
    }

    public byte[] getBufWithOffset() {
        return Arrays.copyOfRange(buf, offset, buf.length);
    }

    public int getSlice(int index) {
        return buf[index + this.offset] & 0xFF;
    }

    public byte[] getSlice(int start, int end) {
        return Arrays.copyOfRange(buf, start + this.offset, end + this.offset);
    }

    public int[] decodeULEB128(int offset) {
        int myInt = 0;
        int multiplier = 1;
        for (int i = 0; i < 5; i++) {
            int b = this.getSlice(offset + i);
            if (i > 0) {
                if (b == 0) {
                    throw new RuntimeException(String.format("Null byte in decodeULEB128: buf=%s val=%d",
                            getSlice(offset, offset + i + 1), myInt));
                }
            }
            myInt += multiplier * (b & 0x7f);
            if ((b & 0x80) == 0x80) {
                multiplier *= 128;
            } else {
                return new int[]{offset + i + 1, myInt};
            }
        }

        throw new RuntimeException("Failed to read ULEB128, buf[0:5] = " +
                Helper.bytesToHex(this.getSlice(offset, offset + 5)));
    }

    public int[] decodeSLEB128(int offset) {
        int myInt = 0;
        int multiplier = 1;
        for (int i = 0; i < 5; i++) {
            int b = this.getSlice(offset + i);
            if ((b & 0x80) == 0x80) {
                myInt += multiplier * (b & 0x7f);
                multiplier *= 128;
            } else {
                myInt += multiplier * (b & 0x3f);
                if ((b & 0x40) == 0x40) {
                    myInt -= multiplier * 64;
                }
                return new int[]{offset + i + 1, myInt};
            }
        }

        throw new RuntimeException("Failed to read SLEB128, buf[0:5] = " +
                Helper.bytesToHex(this.getSlice(offset, offset + 5)));
    }
}
