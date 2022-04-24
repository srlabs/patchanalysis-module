package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

public class OpcodeHashFull extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile, DexSignatureContext context) {
        return code;
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile, DexSignatureContext context) {
        return String.format("FULL: %s => %s", this.name, Helper.bytesToHex(code));
    }

}
