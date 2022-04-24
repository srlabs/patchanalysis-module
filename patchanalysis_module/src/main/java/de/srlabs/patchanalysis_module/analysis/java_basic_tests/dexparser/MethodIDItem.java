package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.longToIntExact;

public class MethodIDItem {

    public int classIndex;
    int protoIndex;
    public int nameIndex;

    public MethodIDItem(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        this.classIndex = Helper.getUnsignedShort(buf);
        this.protoIndex = Helper.getUnsignedShort(buf);
        this.nameIndex = longToIntExact(Helper.getUnsignedInt(buf));
    }

    public static int getSize() {
        return 2 + 2 + 4;
    }
}
