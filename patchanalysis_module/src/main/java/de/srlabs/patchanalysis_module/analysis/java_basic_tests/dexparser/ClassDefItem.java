package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ClassDefItem {

    long classIdx;
    long accessFlags;
    long superClassIdx;
    long interfacesOff;
    long sourceFileIdx;
    long annotationsOff;
    long classDataOff;
    long staticValuesOff;

    public ClassDefItem(byte[] bytes) {

        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        classIdx = Helper.getUnsignedInt(buf);
        accessFlags = Helper.getUnsignedInt(buf);
        superClassIdx = Helper.getUnsignedInt(buf);
        interfacesOff = Helper.getUnsignedInt(buf);
        sourceFileIdx = Helper.getUnsignedInt(buf);
        annotationsOff = Helper.getUnsignedInt(buf);
        classDataOff = Helper.getUnsignedInt(buf);
        staticValuesOff = Helper.getUnsignedInt(buf);
    }

    public static int getSize() {
        return 8 * 4;
    }
}
