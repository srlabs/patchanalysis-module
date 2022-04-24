package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

/*
    Opcode 0x14 ("CONST") is moving a 32-bit immediate constant to a register. This is often used for Resource
    identifiers (i.e. R.string.something), which are assigned in an automatic way by AAPT during compilation. These
    numbers are highly volatile (often change due to totally unrelated changes) and should therefore be excluded from
    the signature. Resource identifiers are 32-bit integers with format 0xPPTTNNNN where PP=0x7f
 */
public class OpcodeConst extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile, DexSignatureContext context) {
        if (code.length != 6) {
            throw new RuntimeException(String.format("Opcode is not 6 bytes, but instead: %d", code.length));
        }

        byte[] slice = Helper.getSlice(code, 2, 6);
        int constValue = Helper.longToIntExact(Helper.getUnsignedInt(slice));

        if (isResourceIdentifier(constValue)) {
            constValue = 0xBEBAFECA;
        }

        byte[][] byteSlices = {
                Helper.getSlice(code, 0, 2),
                Helper.intToByteArray(constValue)
        };

        return Helper.concatenateBytes(byteSlices);
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile, DexSignatureContext context) {
        if (code.length != 6) {
            throw new RuntimeException(String.format("Opcode is not 6 bytes, but instead: %d", code.length));
        }

        byte[] slice = Helper.getSlice(code, 2, 6);
        int constValue = Helper.longToIntExact(Helper.getUnsignedInt(slice));

        if (isResourceIdentifier(constValue)) {
            return "CONST: Resource identifier redacted";
        } else {
            return String.format("CONST_32bit: reg=%d  val=%s", code[1], Integer.toHexString(constValue));
        }
    }

    /*
        Heuristically checks whether the (32-bit) value is likely a resource identifier.
        The 32-bit value is written as 0xppttnnnn, see https://stackoverflow.com/a/6646113 for more details.
        Heuristics:
        pp: 0x01 for system components, 0x7f for normal applications'
        tt: Resource type, incrementally assigned, 0 < tt < 25
        nnnn: Number of resource, evaluation shows that this is pretty much always below 3000, let's allow anything
        up to 5000 to be on the safe side.
     */
    public boolean isResourceIdentifier(int value) {
        int pp = (value & 0xff000000) >> 24;
        int tt = (value & 0x00ff0000) >> 16;
        int nnnn = value & 0xffff;
        if (pp != 0x01 && pp != 0x7f) {
            return false;
        }

        if (tt > 0 && tt < 25 && nnnn < 5000) {
            return true;
        }

        return false;
    }
}
