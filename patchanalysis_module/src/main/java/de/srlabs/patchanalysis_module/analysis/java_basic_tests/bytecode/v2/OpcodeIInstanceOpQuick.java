package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

public class OpcodeIInstanceOpQuick extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile, DexSignatureContext context) {
        byte[][] byteSlices = {
                Helper.getSlice(code, 0, 2),
                Helper.getSlice(code, 4, 6),
        };

        return Helper.concatenateBytes(byteSlices);
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile, DexSignatureContext context) {

        byte[][] slices = {
                Helper.getSlice(code, 0, 2),
                Helper.getSlice(code, 4, 6)
        };
        String bytesHex = Helper.bytesToHex(Helper.concatenateBytes(slices));
        return String.format("%s => %s", this.name, bytesHex);
    }
}
