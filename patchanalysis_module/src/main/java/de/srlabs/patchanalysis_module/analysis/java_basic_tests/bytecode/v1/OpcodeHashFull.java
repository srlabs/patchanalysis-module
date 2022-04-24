package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

public class OpcodeHashFull extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile) {
        return code;
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile) {
        return "FULL: " + this.name;
    }

}
