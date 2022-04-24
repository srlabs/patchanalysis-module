package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VDEXHeader019 extends VDexHeader {

    byte[] magic = new byte[4];
    byte[] version = new byte[4];
    byte[] dexSectionVersion = new byte[4];
    long numberOfDexFiles;
    long verifierDepsSize;

    public VDEXHeader019(byte[] bytes) {
        super(bytes, VDexFile.VDexHeaderType.VDex_019);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(magic, 0, magic.length);
        buf.get(version, 0, version.length);
        buf.get(dexSectionVersion, 0, dexSectionVersion.length);
        numberOfDexFiles = Helper.getUnsignedInt(buf);
        verifierDepsSize = Helper.getUnsignedInt(buf);
    }

    public static int getSize() {
        return 4 + 4 + 4 + 4 + 4;
    }
}
