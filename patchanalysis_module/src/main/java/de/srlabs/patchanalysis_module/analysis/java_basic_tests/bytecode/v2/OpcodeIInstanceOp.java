package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.FieldIDItem;

public class OpcodeIInstanceOp extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile, DexSignatureContext context) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        FieldIDItem fieldIDItem = dexFile.getFieldIDItem(index);
        byte[] className = context.getAdjustedClassName(dexFile, fieldIDItem.classIndex);
        byte[] fieldName = dexFile.getStringRaw(fieldIDItem.nameIndex);
        fieldName = context.adjustFieldName(fieldName, className);
        byte[] zeroByte = new byte[1];
        byte[][] byteSlices = {
                Helper.getSlice(code, 0, 2),
                Helper.getSlice(code, 4, 6),
                className,
                zeroByte,
                fieldName,
                zeroByte
        };

        return Helper.concatenateBytes(byteSlices);
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile, DexSignatureContext context) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        FieldIDItem fieldIDItem = dexFile.getFieldIDItem(index);
        byte[] className = context.getAdjustedClassName(dexFile, fieldIDItem.classIndex);
        byte[] fieldName = dexFile.getStringRaw(fieldIDItem.nameIndex);
        fieldName = context.adjustFieldName(fieldName, className);

        return String.format("%s: %s.%s", this.name, Helper.bytesToHex(className), Helper.bytesToHex(fieldName));
    }
}
