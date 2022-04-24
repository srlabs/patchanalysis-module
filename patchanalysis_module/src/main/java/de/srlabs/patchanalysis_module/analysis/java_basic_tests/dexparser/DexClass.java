package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.longToIntExact;

public class DexClass extends BufWithOffset {

    DexFile dexFile;
    ClassDefItem header;
    ClassTypeDescriptor classType;
    String className;
    Boolean parsed;
    Map<String, ArrayList<EncodedField>> staticFieldsBySig = new HashMap<>();
    Map<String, ArrayList<EncodedField>> instanceFieldsBySig = new HashMap<>();
    Map<String, EncodedMethod> directMethodBySig = new HashMap<>();
    Map<String, EncodedMethod> virtualMethodBySig = new HashMap<>();
    int staticFieldsSize;
    int instanceFieldsSize;
    int directMethodsSize;
    int virtualMethodsSize;

    public DexClass(DexFile dexFile, int offset) {
        super(dexFile, offset);
        this.dexFile = dexFile;
        header = new ClassDefItem(this.getSlice(0, ClassDefItem.getSize()));
        classType = (ClassTypeDescriptor) dexFile.getType(longToIntExact(header.classIdx));
        className = classType.getClassName();
        parsed = false;
    }

    public DexClass(DexFile dexFile) {
        this(dexFile, 0);
    }

    public void parse() {
        if (this.parsed) {
            return;
        }
        this.parsed = true;

        if (this.header.classDataOff == 0) {
            return;
        }

        int pos = longToIntExact(this.header.classDataOff);
        int[] staticFieldsSize = this.dexFile.sharedData.decodeULEB128(pos);
        int[] instanceFieldsSize = this.dexFile.sharedData.decodeULEB128(staticFieldsSize[0]);
        int[] directMethodsSize = this.dexFile.sharedData.decodeULEB128(instanceFieldsSize[0]);
        int[] virtualMethodsSize = this.dexFile.sharedData.decodeULEB128(directMethodsSize[0]);
        this.staticFieldsSize = staticFieldsSize[1];
        this.instanceFieldsSize = instanceFieldsSize[1];
        this.directMethodsSize = directMethodsSize[1];
        this.virtualMethodsSize = virtualMethodsSize[1];
        pos = virtualMethodsSize[0];
        int fieldIndex = 0;

        for (int i = 0; i < this.staticFieldsSize; i++) {
            int[] results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int fieldIndexDiff = results[1];
            fieldIndex += fieldIndexDiff;

            results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int accessFlags = results[1];

            EncodedField field = new EncodedField(this.dexFile, fieldIndex, accessFlags);
            String fieldSignature = field.getSig();
            // putIfAbsent() is not available in API level < 24
            if (!(this.staticFieldsBySig.containsKey(fieldSignature))) {
                this.staticFieldsBySig.put(fieldSignature, new ArrayList<EncodedField>());
            }
            this.staticFieldsBySig.get(fieldSignature).add(field);
        }

        fieldIndex = 0;
        for (int i = 0; i < this.instanceFieldsSize; i++) {
            int[] results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int fieldIndexDiff = results[1];
            fieldIndex += fieldIndexDiff;

            results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int accessFlags = results[1];
            EncodedField field = new EncodedField(this.dexFile, fieldIndex, accessFlags);
            String fieldSignature = field.getSig();
            if (!(this.instanceFieldsBySig.containsKey(fieldSignature))) {
                this.instanceFieldsBySig.put(fieldSignature, new ArrayList<EncodedField>());
            }
            this.instanceFieldsBySig.get(fieldSignature).add(field);
        }

        int methodIndex = 0;
        for (int i = 0; i < this.directMethodsSize; i++) {
            int[] results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int methodIndexDiff = results[1];
            methodIndex += methodIndexDiff;

            results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int accessFlags = results[1];

            results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int codeOff = results[1];

            EncodedMethod method = new EncodedMethod(this.dexFile, methodIndex, accessFlags, codeOff);
            String methodSignature = method.getMethodSig();
            if (this.directMethodBySig.containsKey(methodSignature)) {
                throw new RuntimeException(String.format("Duplicate method %s: %s",
                        this.className, method.getMethodSig()));
            }
            this.directMethodBySig.put(methodSignature, method);
        }
        methodIndex = 0;
        for (int i = 0; i < this.virtualMethodsSize; i++) {
            int[] results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int methodIndexDiff = results[1];
            methodIndex += methodIndexDiff;

            results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int accessFlags = results[1];

            results = this.dexFile.sharedData.decodeULEB128(pos);
            pos = results[0];
            int codeOff = results[1];

            EncodedMethod method = new EncodedMethod(this.dexFile, methodIndex, accessFlags, codeOff);
            AccessFlagsMethod flags = method.getFlags();
            if (flags.hasFlag("SYNTHETIC")) {
                continue;
            }
            String methodSignature = method.getMethodSig();
            if (this.virtualMethodBySig.containsKey(methodSignature)) {
                throw new RuntimeException(String.format("Duplicate method %s: %s",
                        this.className, method.getMethodSig()));
            }
            if (this.directMethodBySig.containsKey(methodSignature)) {
                throw new RuntimeException(String.format("Duplicate method (virtual+direct) %s: %s",
                        this.className, method.getMethodSig()));
            }
            this.virtualMethodBySig.put(methodSignature, method);
        }
    }

    public ArrayList<EncodedField> getInstanceFields(String sig) {
        this.parse();
        return this.instanceFieldsBySig.get(sig);
    }

    public ArrayList<EncodedField> getStaticFields(String sig) {
        this.parse();
        return this.staticFieldsBySig.get(sig);
    }

    public Boolean hasMethod(String methodSpec) {
        this.parse();
        return this.virtualMethodBySig.containsKey(methodSpec) || this.directMethodBySig.containsKey(methodSpec);
    }

    public EncodedMethod getMethod(String methodSpec) {
        this.parse();
        if (this.virtualMethodBySig.containsKey(methodSpec)) {
            return this.virtualMethodBySig.get(methodSpec);
        } else {
            return this.directMethodBySig.get(methodSpec);
        }
    }
}
