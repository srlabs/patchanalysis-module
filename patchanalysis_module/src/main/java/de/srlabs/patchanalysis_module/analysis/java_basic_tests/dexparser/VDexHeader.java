package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VDexHeader extends TypedContainer {

    // 006_010 & 019 common fields
    byte[] magic = new byte[4];
    byte[] version = new byte[4];
    long numberOfDexFiles;
    long verifierDepsSize;

    // 006_010 specific fields
    long dexSize;
    long quickeningInfoSize;

    // 019 specific fields
    byte[] dexSectionVersion = new byte[4];

    VDexFile.VDexHeaderType headerType;

    public VDexHeader(byte[] bytes, VDexFile.VDexHeaderType type) {

        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (type == VDexFile.VDexHeaderType.VDex_006_010) {
            buf.get(magic, 0, magic.length);
            buf.get(version, 0, version.length);
            numberOfDexFiles = Helper.getUnsignedInt(buf);
            dexSize = Helper.getUnsignedInt(buf);
            verifierDepsSize = Helper.getUnsignedInt(buf);
            quickeningInfoSize = Helper.getUnsignedInt(buf);
        } else if (type == VDexFile.VDexHeaderType.VDex_019) {
            buf.get(magic, 0, magic.length);
            buf.get(version, 0, version.length);
            buf.get(dexSectionVersion, 0, dexSectionVersion.length);
            numberOfDexFiles = Helper.getUnsignedInt(buf);
            verifierDepsSize = Helper.getUnsignedInt(buf);
        }

        headerType = type;
    }

    public static int getSize(VDexFile.VDexHeaderType headerType) {
        if (headerType == VDexFile.VDexHeaderType.VDex_006_010) {
            return 4 + 4 + 4 + 4 + 4 + 4;
        } else {
            return 4 + 4 + 4 + 4 + 4;
        }
    }
}
