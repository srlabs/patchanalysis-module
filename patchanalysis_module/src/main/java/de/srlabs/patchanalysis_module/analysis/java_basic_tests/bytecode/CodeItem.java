package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode;

import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1.Opcode;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v1.OpcodeList;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2.DexSignatureContext;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.BufWithOffset;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;

import java.util.Arrays;

import static java.lang.Math.abs;
import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.longToIntExact;

import android.util.Log;


public class CodeItem extends BufWithOffset {

    DexFile dexFile;
    String signatureV1 = null;
    String signatureV2 = null;
    CodeItemHeader codeItem;
    BufWithOffset insns;

    public static CodeItem initCodeItem(DexFile dexFile, int offset) {
        CodeItem item;
        if (dexFile.isCDEX()) {
            item = new CodeItem(dexFile, dexFile.sharedData, offset);
            item.codeItem = new CodeItemHeader(new byte[CodeItemHeader.getSize()]);

            // fields
            byte[] fieldsSlice = item.getSlice(0, 2);
            int fields = Helper.getUnsignedShort(fieldsSlice);

            // insnsCountAndFlags
            byte[] insnsCountAndFlagsSlice = item.getSlice(2, 4);
            int insnsCountAndFlags = Helper.getUnsignedShort(insnsCountAndFlagsSlice);

            int kRegistersSizeShift = 12;
            int kInsSizeShift = 8;
            int kOutsSizeShift = 4;
            int kTriesSizeShift = 0;

            item.codeItem.registersSize = (fields >> kRegistersSizeShift) & 0xF;
            item.codeItem.insSize = (fields >> kInsSizeShift) & 0xF;
            item.codeItem.outsSize = (fields >> kOutsSizeShift) & 0xF;
            item.codeItem.triesSize = (fields >> kTriesSizeShift) & 0xF;

            int kInsnsSizeShift = 5;
            item.codeItem.insnsSize = insnsCountAndFlags >> kInsnsSizeShift;
            int kFlagPreHeaderRegisterSize = 0x1 << 0;
            int kFlagPreHeaderInsSize = 0x1 << 1;
            int kFlagPreHeaderOutsSize = 0x1 << 2;
            int kFlagPreHeaderTriesSize = 0x1 << 3;
            int kFlagPreHeaderInsnsSize = 0x1 << 4;
            int kFlagPreHeaderCombined = kFlagPreHeaderRegisterSize | kFlagPreHeaderInsSize | kFlagPreHeaderOutsSize | kFlagPreHeaderTriesSize | kFlagPreHeaderInsnsSize;

            if ((insnsCountAndFlags & kFlagPreHeaderCombined) != 0) {
                int preHeader = offset;
                if ((insnsCountAndFlags & kFlagPreHeaderInsnsSize) != 0) {
                    preHeader -= 2;
                    byte[] slice = item.dexFile.sharedData.getSlice(preHeader, preHeader + 2);
                    int sliceValue = Helper.getUnsignedShort(slice);
                    item.codeItem.insnsSize += sliceValue;

                    preHeader -= 2;
                    slice = item.dexFile.sharedData.getSlice(preHeader, preHeader + 2);
                    sliceValue = Helper.getUnsignedShort(slice);
                    item.codeItem.insnsSize += sliceValue >> 16;
                }

                if ((insnsCountAndFlags & kFlagPreHeaderRegisterSize) != 0) {
                    preHeader -= 2;
                    byte[] slice = item.dexFile.sharedData.getSlice(preHeader, preHeader + 2);
                    int sliceValue = Helper.getUnsignedShort(slice);
                    item.codeItem.registersSize += sliceValue;
                }

                if ((insnsCountAndFlags & kFlagPreHeaderInsSize) != 0) {
                    preHeader -= 2;
                    byte[] slice = item.dexFile.sharedData.getSlice(preHeader, preHeader + 2);
                    int sliceValue = Helper.getUnsignedShort(slice);
                    item.codeItem.insSize += sliceValue;
                }

                if ((insnsCountAndFlags & kFlagPreHeaderOutsSize) != 0) {
                    preHeader -= 2;
                    byte[] slice = item.dexFile.sharedData.getSlice(preHeader, preHeader + 2);
                    int sliceValue = Helper.getUnsignedShort(slice);
                    item.codeItem.outsSize += sliceValue;
                }

                if ((insnsCountAndFlags & kFlagPreHeaderTriesSize) != 0) {
                    preHeader -= 2;
                    byte[] slice = item.dexFile.sharedData.getSlice(preHeader, preHeader + 2);
                    int sliceValue = Helper.getUnsignedShort(slice);
                    item.codeItem.triesSize += sliceValue;
                }
            }
            item.insns = new BufWithOffset(item, 4);
        } else {
            item = new CodeItem(dexFile, dexFile, offset);
            byte[] codeItemSlice = item.getSlice(0, CodeItemHeader.getSize());
            item.codeItem = new CodeItemHeader(codeItemSlice);
            item.insns = new BufWithOffset(item, CodeItemHeader.getSize());
        }
        return item;
    }

    public CodeItem(DexFile dexFile, BufWithOffset buf, int offset) {
        super(buf, offset);
        this.dexFile = dexFile;
    }

    public String getSignature(String signatureVersion, boolean strictMode) {
        if (signatureVersion.equals("DEXSIG_V1")) {
            if (this.signatureV1 == null) {
                this.signatureV1 = (String) this.hashDEXSIGV1(null, null, strictMode);
            }
            return this.signatureV1;

        } else if (signatureVersion.equals("DEXSIG_V2")) {
            if (this.signatureV2 == null) {
                this.signatureV2 = (String) this.hashDEXSIGV2(null, null, strictMode);
            }
            return this.signatureV2;
        } else {
            throw new RuntimeException("Invalid signature version: " + signatureVersion);
        }
    }

    // Method returns String or CodeHasher object based on parameter -> therefore return type is Object
    public Object hashDEXSIGV1(Boolean verbose, Boolean returnHasher, Boolean strictMode) {
        if (verbose == null) {
            verbose = false;
        }
        if (returnHasher == null) {
            returnHasher = false;
        }
        if (strictMode == null) {
            strictMode = false;
        }

        if (verbose) {
            Log.d(Constants.LOG_TAG, "codeItem=" + this.codeItem);
        }

        OpcodeList opcodeList = new OpcodeList();
        CodeHasher hasher = new CodeHasher("DEXSIG_V1");
        hasher.addUInt16(this.codeItem.registersSize, "registersSize");
        hasher.addUInt16(this.codeItem.insSize, "insSize");
        hasher.addUInt16(this.codeItem.outsSize, "outsSize");
        hasher.addUInt16(this.codeItem.triesSize, "triesSize");
        // Explicitly not hashing debugInfoOff
        hasher.addUInt32(this.codeItem.insnsSize, "insnsSize");

        if (this.codeItem.triesSize > 0) {
            int triesPos = CodeItemHeader.getSize() + 2 * longToIntExact(this.codeItem.insnsSize);
            if (this.dexFile.isCDEX()) {
                triesPos = 4 + 2 * longToIntExact(this.codeItem.insnsSize);
            }
            if ((triesPos + this.offset) % 4 == 2) {
                byte[] slice = this.getSlice(triesPos, triesPos + 2);
                if (!(slice[0] == 0 & slice[1] == 0)) {
                    throw new RuntimeException("Bytes at triesPos are not empty");
                }
                triesPos += 2;
            }

            for (int i = 0; i < this.codeItem.triesSize; i++) {
                byte[] buf = this.getSlice(triesPos + TryItem.getSize() * i, triesPos + TryItem.getSize() * (i + 1));
                hasher.addBinary(buf);
                hasher.addText(String.format("tryItem_%d=%s", i, new TryItem(buf)));
            }

            int pos = triesPos + TryItem.getSize() * this.codeItem.triesSize;
            int encodedCatchHandlerListStart = pos;
            int[] result = this.decodeULEB128(pos);
            pos = result[0];
            int numEncodedCatchHandlers = result[1];
            hasher.addUInt32(numEncodedCatchHandlers, "numEncodedCatchHandlers");

            for (int catchHandlerIndex = 0; catchHandlerIndex < numEncodedCatchHandlers; catchHandlerIndex++) {
                hasher.addText(String.format("catch_handlers[%d] at pos %d", catchHandlerIndex, pos - encodedCatchHandlerListStart));
                result = this.decodeSLEB128(pos);
                pos = result[0];
                int size = result[1];
                hasher.addInt32(size, String.format("catch_handlers[%d].size", catchHandlerIndex));

                for (int i = 0; i < abs(size); i++) {
                    result = this.decodeULEB128(pos);
                    pos = result[0];
                    int typeIdx = result[1];

                    byte[] typeStrRaw = this.dexFile.getTypeDescriptorRaw(typeIdx);
                    byte[] typeStrRawNull = Arrays.copyOf(typeStrRaw, typeStrRaw.length + 1);
                    typeStrRawNull[typeStrRaw.length] = 0;
                    hasher.addBinary(typeStrRawNull);
                    hasher.addText(String.format("catch_handlers[%d][%d].type=%s", catchHandlerIndex, i, this.dexFile.getType(typeIdx)));
                    result = this.decodeULEB128(pos);
                    pos = result[0];
                    int addr = result[1];
                    hasher.addUInt32(addr, String.format("catch_handlers[%d][%d].addr", catchHandlerIndex, i));
                }
                if (size <= 0) {
                    result = this.decodeULEB128(pos);
                    pos = result[0];
                    int catchAllAddr = result[1];
                    hasher.addUInt32(catchAllAddr, String.format("catch_all_addr"));
                }
            }
        }

        int pos = 0;
        while (pos < this.codeItem.insnsSize) {
            int opcodeNum = this.insns.getSlice(2 * pos + 0);
            if (opcodeNum == 0 && this.insns.getSlice(2 * pos + 1) != 0) {
                int pseudoType = this.insns.getSlice(2 * pos + 1);
                int numCodeUnits = 0;
                int size;
                if (pseudoType == 1) {
                    byte[] slice = this.insns.getSlice(2 * pos + 2, 2 * pos + 4);
                    size = Helper.getUnsignedShort(slice);
                    numCodeUnits = (size * 2) + 4;
                } else if (pseudoType == 2) {
                    byte[] slice = this.insns.getSlice(2 * pos + 2, 2 * pos + 4);
                    size = Helper.getUnsignedShort(slice);
                    numCodeUnits = (size * 4) + 2;
                } else if (pseudoType == 3) {
                    byte[] elementWidthSlice = this.insns.getSlice(2 * pos + 2, 2 * pos + 4);
                    int elementWidth = Helper.getUnsignedShort(elementWidthSlice);

                    byte[] sizeSlice = this.insns.getSlice(2 * pos + 4, 2 * pos + 6);
                    size = Helper.getUnsignedShort(sizeSlice);
                    numCodeUnits = ((size * elementWidth + 1) / 2) + 4;
                } else {
                    throw new RuntimeException("Invalid pseudo-instruction: " + pseudoType);
                }

                if (verbose) {
                    Log.d(Constants.LOG_TAG, String.format("PSEUDO: pos=%d pseudoType=%d numCodeUnits=%d", pos, pseudoType, numCodeUnits));
                }
                byte[] bufSlice = this.insns.getSlice(2 * pos, 2 * (pos + numCodeUnits));
                hasher.addBinary(bufSlice);
                hasher.addText(String.format("Pseudo %d: %s", pseudoType, Helper.bytesToHex(bufSlice)));
                pos += numCodeUnits;
            } else {
                Opcode opcode = opcodeList.get(opcodeNum);
                // Enforce that all opcodes are valid only in strict mode, production test should not enforce it when calculating signatures
                if (strictMode & !opcode.isHashValid()) {
                    throw new RuntimeException(String.format("Hash not valid for %s (0x%02x)", opcode.name, opcodeNum));
                }

                byte[] opcodeBuf = this.insns.getSlice(2 * pos, 2 * (pos + opcode.length));
                try {
                    hasher.addBinary(opcode.hashBinary(opcodeBuf, this.dexFile));
                    hasher.addText(opcode.hashText(opcodeBuf, this.dexFile));
                } catch (Exception e) {
                    hasher.dump();
                    throw new RuntimeException(String.format("Exception %s at pos %d => opcode %s => buf %s", e, pos, opcode.name, Helper.bytesToHex(opcodeBuf)), e);
                }
                pos += opcode.length;
            }
        }

        if (verbose) {
            hasher.dump();
        }

        if (returnHasher) {
            return hasher;
        } else {
            return hasher.getBinaryHash();
        }
    }

    public Object hashDEXSIGV2(Boolean verbose, Boolean returnHasher, Boolean strictMode) {
        if (verbose == null) {
            verbose = false;
        }
        if (returnHasher == null) {
            returnHasher = false;
        }
        if (strictMode == null) {
            strictMode = false;
        }

        if (verbose) {
            Log.d(Constants.LOG_TAG, "codeItem=" + this.codeItem);
        }

        DexSignatureContext signatureContext = new DexSignatureContext();
        de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2.OpcodeList opcodeList =
                new de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2.OpcodeList();
        CodeHasher hasher = new CodeHasher("DEXSIG_V2");
        hasher.addUInt16(this.codeItem.registersSize, "registersSize");
        hasher.addUInt16(this.codeItem.insSize, "insSize");
        hasher.addUInt16(this.codeItem.outsSize, "outsSize");
        hasher.addUInt16(this.codeItem.triesSize, "triesSize");
        // Explicitly not hashing debugInfoOff
        hasher.addUInt32(this.codeItem.insnsSize, "insnsSize");

        if (this.codeItem.triesSize > 0) {
            int triesPos = CodeItemHeader.getSize() + 2 * longToIntExact(this.codeItem.insnsSize);
            if (this.dexFile.isCDEX()) {
                triesPos = 4 + 2 * longToIntExact(this.codeItem.insnsSize);
            }
            if ((triesPos + this.offset) % 4 == 2) {
                byte[] slice = this.getSlice(triesPos, triesPos + 2);
                if (!(slice[0] == 0 & slice[1] == 0)) {
                    throw new RuntimeException("Bytes at triesPos are not empty");
                }
                triesPos += 2;
            }

            for (int i = 0; i < this.codeItem.triesSize; i++) {
                byte[] buf = this.getSlice(triesPos + TryItem.getSize() * i, triesPos + TryItem.getSize() * (i + 1));
                hasher.addBinary(buf);
                hasher.addText(String.format("tryItem_%d=%s", i, new TryItem(buf)));
            }

            int pos = triesPos + TryItem.getSize() * this.codeItem.triesSize;
            int encodedCatchHandlerListStart = pos;
            int[] result = this.decodeULEB128(pos);
            pos = result[0];
            int numEncodedCatchHandlers = result[1];
            hasher.addUInt32(numEncodedCatchHandlers, "numEncodedCatchHandlers");

            for (int catchHandlerIndex = 0; catchHandlerIndex < numEncodedCatchHandlers; catchHandlerIndex++) {
                hasher.addText(String.format("catch_handlers[%d] at pos %d", catchHandlerIndex, pos - encodedCatchHandlerListStart));
                result = this.decodeSLEB128(pos);
                pos = result[0];
                int size = result[1];
                hasher.addInt32(size, String.format("catch_handlers[%d].size", catchHandlerIndex));

                for (int i = 0; i < abs(size); i++) {
                    result = this.decodeULEB128(pos);
                    pos = result[0];
                    int typeIdx = result[1];

                    byte[] typeStrRaw = this.dexFile.getTypeDescriptorRaw(typeIdx);
                    byte[] typeStrRawNull = Arrays.copyOf(typeStrRaw, typeStrRaw.length + 1);
                    typeStrRawNull[typeStrRaw.length] = 0;
                    hasher.addBinary(typeStrRawNull);
                    hasher.addText(String.format("catch_handlers[%d][%d].type=%s", catchHandlerIndex, i, this.dexFile.getType(typeIdx)));

                    result = this.decodeULEB128(pos);
                    pos = result[0];
                    int addr = result[1];
                    hasher.addUInt32(addr, String.format("catch_handlers[%d][%d].addr", catchHandlerIndex, i));
                }
                if (size <= 0) {
                    result = this.decodeULEB128(pos);
                    pos = result[0];
                    int catchAllAddr = result[1];
                    hasher.addUInt32(catchAllAddr, String.format("catch_all_addr"));
                }
            }
        }

        int pos = 0;
        while (pos < this.codeItem.insnsSize) {
            int opcodeNum = this.insns.getSlice(2 * pos + 0);
            if (opcodeNum == 0 && this.insns.getSlice(2 * pos + 1) != 0) {
                int pseudoType = this.insns.getSlice(2 * pos + 1);
                int numCodeUnits = 0;
                int size;
                if (pseudoType == 1) {
                    byte[] slice = this.insns.getSlice(2 * pos + 2, 2 * pos + 4);
                    size = Helper.getUnsignedShort(slice);
                    numCodeUnits = (size * 2) + 4;
                } else if (pseudoType == 2) {
                    byte[] slice = this.insns.getSlice(2 * pos + 2, 2 * pos + 4);
                    size = Helper.getUnsignedShort(slice);
                    numCodeUnits = (size * 4) + 2;
                } else if (pseudoType == 3) {
                    byte[] elementWidthSlice = this.insns.getSlice(2 * pos + 2, 2 * pos + 4);
                    int elementWidth = Helper.getUnsignedShort(elementWidthSlice);

                    byte[] sizeSlice = this.insns.getSlice(2 * pos + 4, 2 * pos + 6);
                    size = Helper.getUnsignedShort(sizeSlice);
                    numCodeUnits = ((size * elementWidth + 1) / 2) + 4;
                } else {
                    throw new RuntimeException("Invalid pseudo-instruction: " + pseudoType);
                }

                if (verbose) {
                    Log.d(Constants.LOG_TAG, String.format("PSEUDO: pos=%d pseudoType=%d numCodeUnits=%d", pos, pseudoType, numCodeUnits));
                }

                byte[] bufSlice = this.insns.getSlice(2 * pos, 2 * (pos + numCodeUnits));
                hasher.addBinary(bufSlice);
                hasher.addText(String.format("Pseudo %d: %s", pseudoType, Helper.bytesToHex(bufSlice)));
                pos += numCodeUnits;
            } else {
                de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2.Opcode opcode = opcodeList.get(opcodeNum);
                // Enforce that all opcodes are valid only in strict mode, production test should not enforce it when calculating signatures
                if (strictMode & !opcode.isHashValid()) {
                    throw new RuntimeException(String.format("Hash not valid for %s (0x%02x)", opcode.name, opcodeNum));
                }
                byte[] opcodeBuf = this.insns.getSlice(2 * pos, 2 * (pos + opcode.length));
                try {
                    hasher.addBinary(opcode.hashBinary(opcodeBuf, this.dexFile, signatureContext));
                    hasher.addText(opcode.hashText(opcodeBuf, this.dexFile, signatureContext));
                } catch (Exception e) {
                    hasher.dump();
                    throw new RuntimeException(String.format("Exception %s at pos %d => opcode %s => buf %s", e, pos, opcode.name, Helper.bytesToHex(opcodeBuf)), e);
                }
                pos += opcode.length;
            }
        }

        if (verbose) {
            hasher.dump();
        }

        if (returnHasher) {
            return hasher;
        } else {
            return hasher.getBinaryHash();
        }
    }

}
