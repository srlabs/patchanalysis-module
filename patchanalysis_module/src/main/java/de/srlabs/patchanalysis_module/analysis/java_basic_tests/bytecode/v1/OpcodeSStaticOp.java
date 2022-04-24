package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.FieldIDItem;

public class OpcodeSStaticOp extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        FieldIDItem fieldIDItem = dexFile.getFieldIDItem(index);
        byte[] className = dexFile.getTypeDescriptorRaw(fieldIDItem.classIndex);
        byte[] fieldName = dexFile.getStringRaw(fieldIDItem.nameIndex);
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
    public String hashText(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        FieldIDItem fieldIDItem = dexFile.getFieldIDItem(index);
        String className = dexFile.getType(fieldIDItem.classIndex).toString();
        String fieldName = dexFile.getString(fieldIDItem.nameIndex);

        return String.format("%s: %s.%s", this.name, className, fieldName);
    }
}
