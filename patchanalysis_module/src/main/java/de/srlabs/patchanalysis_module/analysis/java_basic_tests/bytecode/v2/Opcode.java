package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

import java.util.Map;

public class Opcode {

    public String name;
    String format;
    public int opcode;
    public int length;
    Boolean hashValid;

    public static Opcode create(Map<String, Object> opcodeItem) {
        int opcodeNum = (Integer) opcodeItem.get("opcodeNum");
        String opcodeName = (String) opcodeItem.get("opcodeName");
        Opcode opcode;
        if (OpcodeList.opcodeClasses.containsKey(opcodeName)) {
            try {
                Class opcodeClass = OpcodeList.opcodeClasses.get(opcodeName);
                opcode = (Opcode) opcodeClass.newInstance();
                opcode.hashValid = true;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException("Could not create Opcode instance", e);
            }
            // Some classes/ranges of opcodes
        } else if (0x7b <= opcodeNum & opcodeNum <= 0x8f) {
            // unop
            opcode = new OpcodeHashFull();
            opcode.hashValid = true;
        } else if (0x90 <= opcodeNum & opcodeNum <= 0xaf) {
            // binop
            opcode = new OpcodeHashFull();
            opcode.hashValid = true;
        } else if (0xb0 <= opcodeNum & opcodeNum <= 0xcf) {
            // binop/2addr
            opcode = new OpcodeHashFull();
            opcode.hashValid = true;
        } else if (0xd0 <= opcodeNum & opcodeNum <= 0xd7) {
            // binop/lit16
            opcode = new OpcodeHashFull();
            opcode.hashValid = true;
        } else if (0xd8 <= opcodeNum & opcodeNum <= 0xe2) {
            // binop/lit8
            opcode = new OpcodeHashFull();
            opcode.hashValid = true;
        } else {
            opcode = new Opcode();
            opcode.hashValid = false;
        }

        opcode.opcode = opcodeNum;
        opcode.name = opcodeName;
        if (!Helper.stringFollowsRegex(opcodeName, "opcode")) {
            throw new RuntimeException("Opcode name is not valid: " + opcodeName);
        }

        opcode.format = (String) opcodeItem.get("format");
        opcode.length = (Integer) opcodeItem.get("length");

        return opcode;
    }

    public byte[] hashBinary(byte[] code, DexFile dexFile, DexSignatureContext context) {
        return code;
    }

    public String hashText(byte[] code, DexFile dexFile, DexSignatureContext context) {
        return String.format("UNKNOWN: %s (0x%02x)", this.name, this.opcode);
    }

    public boolean isHashValid() {
        return this.hashValid;
    }
}
