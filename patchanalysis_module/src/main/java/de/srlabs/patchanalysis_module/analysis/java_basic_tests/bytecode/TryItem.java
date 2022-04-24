package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.TypedContainer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TryItem extends TypedContainer {

    public long startAddr;
    public int insnCount;
    public int handlerOff;

    public TryItem(byte[] bytes) {
        Helper.printBytes(bytes);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        startAddr = Helper.getUnsignedInt(buf);
        insnCount = Helper.getUnsignedShort(buf);
        handlerOff = Helper.getUnsignedShort(buf);
    }

    public static int getSize() {
        return 4 + 2 + 2;
    }
}
