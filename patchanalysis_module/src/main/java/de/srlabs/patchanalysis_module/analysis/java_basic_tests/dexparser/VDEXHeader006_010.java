package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VDEXHeader006_010 extends VDexHeader {

    byte[] magic = new byte[4];
    byte[] version = new byte[4];
    long numberOfDexFiles;
    long dexSize;
    long verifierDepsSize;
    long quickeningInfoSize;

    public VDEXHeader006_010(byte[] bytes) {
        super(bytes, VDexFile.VDexHeaderType.VDex_006_010);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(magic, 0, magic.length);
        buf.get(version, 0, version.length);
        numberOfDexFiles = Helper.getUnsignedInt(buf);
        dexSize = Helper.getUnsignedInt(buf);
        verifierDepsSize = Helper.getUnsignedInt(buf);
        quickeningInfoSize = Helper.getUnsignedInt(buf);
    }

    public static int getSize() {
        return 4 + 4 + 4 + 4 + 4 + 4;
    }
}
