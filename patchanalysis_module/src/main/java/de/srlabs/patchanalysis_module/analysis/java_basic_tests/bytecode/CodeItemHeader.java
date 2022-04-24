package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class CodeItemHeader {

    int registersSize;
    int insSize;
    int outsSize;
    int triesSize;
    long debugInfoOff;
    long insnsSize;

    public CodeItemHeader(byte[] bytes) {
        Helper.printBytes(bytes);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        registersSize = Helper.getUnsignedShort(buf);
        insSize = Helper.getUnsignedShort(buf);
        outsSize = Helper.getUnsignedShort(buf);
        triesSize = Helper.getUnsignedShort(buf);
        debugInfoOff = Helper.getUnsignedInt(buf);
        insnsSize = Helper.getUnsignedInt(buf);
    }

    public static int getSize() {
        return 2 + 2 + 2 + 2 + 4 + 4;
    }
}
