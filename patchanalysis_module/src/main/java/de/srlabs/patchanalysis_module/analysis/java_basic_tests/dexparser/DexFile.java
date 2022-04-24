package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.CodeItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.longToIntExact;

public class DexFile extends BufWithOffset {

    enum DEXFileType {
        DEX,
        CDEX
    }

    Map<String, DexClass> classesByName = new HashMap<>();
    Map<Long, DexClass> classesByIdx = new HashMap<>();
    public DexHeader header;
    DEXFileType fileType;
    public BufWithOffset sharedData;

    Map<Integer, CodeItem> codeCache = new HashMap<>();

    public DexFile(byte[] buf, int offset, BufWithOffset sharedData) {
        super(buf, offset);

        if (sharedData == null) {
            this.sharedData = this;
        } else {
            this.sharedData = sharedData;
        }

        byte[] magic = this.getSlice(0, 4);
        String magicString = new String(magic, StandardCharsets.UTF_8);
        if (magicString.equals("dex\n")) {
            header = new DexHeader(this, DEXFileType.DEX);
            fileType = DEXFileType.DEX;
        } else if (magicString.equals("cdex")) {
            header = new DexHeader(this, DEXFileType.CDEX);
            fileType = DEXFileType.CDEX;
        }

        // 305419896_10 = 0x12345678_16
        if (header.endianTag == 305419896) {
            for (int i = 0; i < header.classDefsSize; i++) {
                DexClass myClass = new DexClass(this, longToIntExact(header.classDefsOff) + i * ClassDefItem.getSize());
                classesByName.put(myClass.className, myClass);
                classesByIdx.put(myClass.header.classIdx, myClass);
            }
        }
    }

    public DexFile(byte[] buf, DexFile sharedData) {
        this(buf, 0, sharedData);
    }

    public DexFile(byte[] buf, int offset) {
        this(buf, offset, null);
    }

    public DexFile(byte[] buf) {
        this(buf, 0, null);
    }

    public Boolean hasDexClass(String className) {
        return classesByName.containsKey(className);
    }

    public DexClass getDexClass(String className) {
        return classesByName.get(className);

    }

    public TypeDescriptor getType(int typeIdx) {
        if (typeIdx < 0 || typeIdx > this.header.typeIDsSize) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "typeIdx %d not in range 0..%d", typeIdx, this.header.typeIDsSize));
        }
        byte[] descriptorSlice = this.getSlice(longToIntExact(header.typeIDsOff + 4 * typeIdx), longToIntExact(header.typeIDsOff + 4 * typeIdx + 4));
        long descriptorStringIdx = Helper.getUnsignedInt(descriptorSlice);
        String descriptorString = getString(descriptorStringIdx);
        TypeDescriptor t = TypeDescriptor.parse(descriptorString);
        return t;
    }

    public byte[] getTypeDescriptorRaw(int typeIdx) {
        if (typeIdx < 0 || typeIdx >= this.header.typeIDsSize) {
            throw new IndexOutOfBoundsException(String.format(
                    "typeIdx %d out of range 0..%d", typeIdx, this.header.typeIDsSize));
        } else {
            byte[] slice = this.getSlice(longToIntExact(this.header.typeIDsOff) + 4 * typeIdx, longToIntExact(this.header.typeIDsOff) + 4 * typeIdx + 4);
            long descriptorStringIdx = Helper.getUnsignedInt(slice);
            return this.getStringRaw(descriptorStringIdx);
        }
    }

    public TypeDescriptor[] getTypeList(int pos) {
        if (pos == 0) {
            return new TypeDescriptor[]{};
        }
        byte[] sizeSlice = this.sharedData.getSlice(pos, pos + 4);
        long size = Helper.getUnsignedInt(sizeSlice);
        ArrayList<TypeDescriptor> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            byte[] typeIndexSlice = this.sharedData.getSlice(pos + 4 + 2 * i, pos + 6 + 2 * i);
            int typeIndex = Helper.getUnsignedShort(typeIndexSlice);
            result.add(this.getType(typeIndex));
        }

        return result.toArray(new TypeDescriptor[result.size()]);
    }

    public String getString(long stringIdx) {
        byte[] rawString = getStringRaw(stringIdx);
        return new String(rawString, StandardCharsets.UTF_8);
    }

    public byte[] getStringRaw(long stringIdx) {
        if (stringIdx < 0 || stringIdx >= this.header.stringIDsSize) {
            throw new IndexOutOfBoundsException(String.format(
                    "stringIdx %d out of range 0..%d", stringIdx, this.header.stringIDsSize));
        }
        byte[] stringOffsetSlice = this.getSlice(longToIntExact(header.stringIDsOff + 4 * stringIdx), longToIntExact(header.stringIDsOff + 4 * stringIdx + 4));
        long stringOffset = Helper.getUnsignedInt(stringOffsetSlice);
        int[] result = sharedData.decodeULEB128(longToIntExact(stringOffset));
        int stringStartOffset = result[0];
        int length = result[1];

        byte[] strBufSlice = sharedData.getSlice(stringStartOffset, stringStartOffset + length);
        List<Byte> strBuf = new ArrayList<>();

        int pos = stringStartOffset + length;
        while (true) {
            byte b = (byte) sharedData.getSlice(pos);
            pos += 1;
            if (b == 0) {
                try {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    output.write(strBufSlice);
                    byte[] strBufArray = new byte[strBuf.size()];
                    for (int i = 0; i < strBuf.size(); i++) strBufArray[i] = strBuf.get(i);
                    output.write(strBufArray);
                    return output.toByteArray();
                } catch (IOException io) {
                    throw new RuntimeException("Could not write buffer");
                }
            }
            strBuf.add(b);
        }
    }

    public FieldIDItem getFieldIDItem(int index) {
        int pos = longToIntExact(this.header.fieldIDsOff) + FieldIDItem.getSize() * index;
        byte[] buf = this.getSlice(pos, pos + FieldIDItem.getSize());
        return new FieldIDItem(buf);
    }

    public MethodIDItem getMethodIDItem(int index) {
        int pos = longToIntExact(this.header.methodIDsOff) + MethodIDItem.getSize() * index;
        byte[] buf = this.getSlice(pos, pos + MethodIDItem.getSize());
        return new MethodIDItem(buf);
    }

    public ProtoIDItem getProto(int index) {
        int pos = longToIntExact(this.header.protoIDsOff) + ProtoIDItem.getSize() * index;
        byte[] buf = this.getSlice(pos, pos + ProtoIDItem.getSize());
        return new ProtoIDItem(buf);
    }

    public Boolean isCDEX() {
        return this.fileType == DEXFileType.CDEX;
    }

    public CodeItem getCode(int pos) {
        if (!this.codeCache.containsKey(pos)) {
            CodeItem item = CodeItem.initCodeItem(this, pos);
            this.codeCache.put(pos, item);
        }
        return this.codeCache.get(pos);
    }


}
