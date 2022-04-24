package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.longToIntExact;

public class ProtoIDItem {

    int shortyIndex;
    int returnTypeIndex;
    int parametersOff;

    public ProtoIDItem(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        this.shortyIndex = longToIntExact(Helper.getUnsignedInt(buf));
        this.returnTypeIndex = longToIntExact(Helper.getUnsignedInt(buf));
        this.parametersOff = longToIntExact(Helper.getUnsignedInt(buf));
    }

    public static int getSize() {
        return 4 + 4 + 4;
    }

}
