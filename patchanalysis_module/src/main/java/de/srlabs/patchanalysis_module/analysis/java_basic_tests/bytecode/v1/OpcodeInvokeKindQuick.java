package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

public class OpcodeInvokeKindQuick extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile) {
        byte[][] byteSlices = {
                Helper.getSlice(code, 0, 2),
                Helper.getSlice(code, 4, 6),
        };

        return Helper.concatenateBytes(byteSlices);
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);

        byte[][] slices = {
                Helper.getSlice(code, 1, 2),
                Helper.getSlice(code, 4, 6)
        };
        String bytesHex = Helper.bytesToHex(Helper.concatenateBytes(slices));
        return String.format("INVOKE_KIND_QUICK (%s) => B=%s  params: %s", this.opcode, index, bytesHex);
    }
}

