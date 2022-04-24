package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import android.text.TextUtils;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.CodeItem;

public class EncodedMethod {

    DexFile dexFile;
    int methodIndex;
    int accessFlags;
    int codeOff;

    public EncodedMethod(DexFile dexFile, int methodIndex, int accessFlags, int codeOff) {
        this.dexFile = dexFile;
        this.methodIndex = methodIndex;
        this.accessFlags = accessFlags;
        this.codeOff = codeOff;
    }

    public MethodIDItem getMethodIDItem() {
        return this.dexFile.getMethodIDItem(this.methodIndex);
    }

    public String getMethodName() {
        return this.dexFile.getString(this.getMethodIDItem().nameIndex);
    }

    public ProtoIDItem getProto() {
        return this.dexFile.getProto(this.getMethodIDItem().protoIndex);
    }

    public TypeDescriptor getReturnType() {
        ProtoIDItem proto = this.getProto();
        return this.dexFile.getType(proto.returnTypeIndex);
    }

    public String getMethodSig() {
        TypeDescriptor returnType = this.getReturnType();
        String methodName = this.getMethodName();
        String paramsStr = this.getParamsStr();

        return returnType + " " + methodName + "(" + paramsStr + ")";
    }

    public String[] getParams() {
        ProtoIDItem proto = this.getProto();
        if (proto.parametersOff == 0) {
            return new String[]{};
        }

        TypeDescriptor[] typeList = this.dexFile.getTypeList(proto.parametersOff);
        String[] params = new String[typeList.length];
        for (int i = 0; i < typeList.length; i++) {
            params[i] = typeList[i].toString();
        }
        return params;
    }

    public String getParamsStr() {
        String[] params = this.getParams();
        return TextUtils.join(", ", params);
    }

    public int getAccessFlags() {
        return this.accessFlags;
    }

    public AccessFlagsMethod getFlags() {
        return new AccessFlagsMethod(this.accessFlags);
    }

    public CodeItem getCode() {
        if (this.codeOff == 0) {
            if (!(new AccessFlagsMethod(this.accessFlags).hasFlag("ABSTRACT") ||
                    new AccessFlagsMethod(this.accessFlags).hasFlag("NATIVE"))) {
                throw new RuntimeException("Method is missing necessary access flags");
            }
            return null;
        }
        return this.dexFile.getCode(this.codeOff);
    }
}
