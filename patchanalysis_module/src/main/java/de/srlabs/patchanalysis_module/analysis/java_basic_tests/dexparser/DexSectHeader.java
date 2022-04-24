package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DexSectHeader extends TypedContainer {
    long dexSize;
    long dexSharedDataSize;
    long quickeningInfoSize;

    public DexSectHeader(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        dexSize = Helper.getUnsignedInt(buf);
        dexSharedDataSize = Helper.getUnsignedInt(buf);
        quickeningInfoSize = Helper.getUnsignedInt(buf);
    }

    public static int getSize() {
        return 4 + 4 + 4;
    }
}
