package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.MethodIDItem;

public class OpcodeInvokeKind extends Opcode {

    @Override
    public byte[] hashBinary(byte[] code, DexFile dexFile, DexSignatureContext context) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        MethodIDItem methodIDItem = dexFile.getMethodIDItem(index);
        byte[] className = context.getAdjustedClassName(dexFile, methodIDItem.classIndex);
        byte[] methodName = dexFile.getStringRaw(methodIDItem.nameIndex);
        methodName = context.adjustMethodName(methodName, className);
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
    }

    @Override
    public String hashText(byte[] code, DexFile dexFile, DexSignatureContext context) {
        byte[] slice = Helper.getSlice(code, 2, 4);
        int index = Helper.getUnsignedShort(slice);
        MethodIDItem methodIDItem = dexFile.getMethodIDItem(index);
        byte[] className = context.getAdjustedClassName(dexFile, methodIDItem.classIndex);
        byte[] methodName = dexFile.getStringRaw(methodIDItem.nameIndex);
        methodName = context.adjustMethodName(methodName, className);

        byte[][] slices = {
                Helper.getSlice(code, 1, 2),
                Helper.getSlice(code, 4, 6)
        };
        String bytesHex = Helper.bytesToHex(Helper.concatenateBytes(slices));
        return String.format("INVOKE_KIND => %s METHOD: %s.%s, params: %s",
                this.name, index, Helper.bytesToHex(className), Helper.bytesToHex(methodName), bytesHex);
    }
}
