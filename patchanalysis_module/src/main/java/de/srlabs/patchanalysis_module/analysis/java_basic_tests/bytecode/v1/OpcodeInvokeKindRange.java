package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.MethodIDItem;

public class OpcodeInvokeKindRange extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        MethodIDItem methodIDItem = dexFile.getMethodIDItem(index);
        try {
            byte[] className = dexFile.getTypeDescriptorRaw(methodIDItem.classIndex);
            byte[] methodName = dexFile.getStringRaw(methodIDItem.nameIndex);
            byte[] zeroByte = new byte[1];
            byte[][] byteSlices = {
                    Helper.getSlice(code, 0, 2),
                    Helper.getSlice(code, 4, 6),
                    className,
                    zeroByte,
                    methodName,
                    zeroByte
            };

            return Helper.concatenateBytes(byteSlices);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(String.format("getTypeDescriptorRaw failed: %d, dexFile.header.typeIdsSize=%d",
                    index, dexFile.header.typeIDsSize), e);
        }
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        MethodIDItem methodIDItem = dexFile.getMethodIDItem(index);
        String className = dexFile.getType(methodIDItem.classIndex).toString();
        String methodName = dexFile.getString(methodIDItem.nameIndex);

        byte[][] slices = {
                Helper.getSlice(code, 1, 2),
                Helper.getSlice(code, 4, 6)
        };
        String bytesHex = Helper.bytesToHex(Helper.concatenateBytes(slices));
        return String.format("INVOKE_KIND => %s METHOD %s.%s, params: %s",
                this.name, index, className, methodName, bytesHex);
    }
}
