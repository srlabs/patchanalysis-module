package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

public class OpcodeConstStringJumbo extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 6);
        long index = Helper.getUnsignedInt(slice);

        try {
            byte[] myStr = dexFile.getStringRaw(index);
            byte[] zeroByte = new byte[1];
            byte[][] byteSlices = {
                    Helper.getSlice(code, 0, 2),
                    myStr,
                    zeroByte
            };
            return Helper.concatenateBytes(byteSlices);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("OpcodeConstStringJumbo: Failed to get string " + index);
        }
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 6);
        long index = Helper.getUnsignedInt(slice);
        String myStr = dexFile.getString(index);

        return String.format("CONST_STRING_JUMBO: %s", myStr);
    }
}
