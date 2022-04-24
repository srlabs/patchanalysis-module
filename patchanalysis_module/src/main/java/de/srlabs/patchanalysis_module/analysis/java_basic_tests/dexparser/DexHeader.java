package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DexHeader extends TypedContainer {

    // dex & cdex common fields
    byte[] magicDex = new byte[4];
    byte[] magicVersion = new byte[4];
    long checksum;
    byte[] signature = new byte[20];
    long fileSize;
    long headerSize;
    long endianTag;
    long linkSize;
    long linkOff;
    long mapOff;
    long stringIDsSize;
    long stringIDsOff;
    public long typeIDsSize;
    long typeIDsOff;
    long protoIDsSize;
    long protoIDsOff;
    long fieldIDsSize;
    long fieldIDsOff;
    long methodIDsSize;
    long methodIDsOff;
    long classDefsSize;
    long classDefsOff;
    long dataSize;
    long dataOff;

    // cdex specific fields
    long featureFlags;
    long debugInfoOffsetsPos;
    long debugInfoOffsetsTableOffset;
    long debugInfoBase;
    long ownedDataBegin;
    long ownedDataEnd;

    DexFile.DEXFileType fileType;

    public DexHeader(BufWithOffset bufRef, DexFile.DEXFileType type) {

        ByteBuffer buf = ByteBuffer.wrap(bufRef.buf, bufRef.offset, 136).order(ByteOrder.LITTLE_ENDIAN);
        buf.slice();
        buf.get(magicDex, 0, magicDex.length);
        buf.get(magicVersion, 0, magicVersion.length);
        checksum = Helper.getUnsignedInt(buf);
        buf.get(signature, 0, signature.length);
        fileSize = Helper.getUnsignedInt(buf);
        headerSize = Helper.getUnsignedInt(buf);
        endianTag = Helper.getUnsignedInt(buf);
        linkSize = Helper.getUnsignedInt(buf);
        linkOff = Helper.getUnsignedInt(buf);
        mapOff = Helper.getUnsignedInt(buf);
        stringIDsSize = Helper.getUnsignedInt(buf);
        stringIDsOff = Helper.getUnsignedInt(buf);
        typeIDsSize = Helper.getUnsignedInt(buf);
        typeIDsOff = Helper.getUnsignedInt(buf);
        protoIDsSize = Helper.getUnsignedInt(buf);
        protoIDsOff = Helper.getUnsignedInt(buf);
        fieldIDsSize = Helper.getUnsignedInt(buf);
        fieldIDsOff = Helper.getUnsignedInt(buf);
        methodIDsSize = Helper.getUnsignedInt(buf);
        methodIDsOff = Helper.getUnsignedInt(buf);
        classDefsSize = Helper.getUnsignedInt(buf);
        classDefsOff = Helper.getUnsignedInt(buf);
        dataSize = Helper.getUnsignedInt(buf);
        dataOff = Helper.getUnsignedInt(buf);

        if (type == DexFile.DEXFileType.CDEX) {
            featureFlags = Helper.getUnsignedInt(buf);
            debugInfoOffsetsPos = Helper.getUnsignedInt(buf);
            debugInfoOffsetsTableOffset = Helper.getUnsignedInt(buf);
            debugInfoBase = Helper.getUnsignedInt(buf);
            ownedDataBegin = Helper.getUnsignedInt(buf);
            ownedDataEnd = Helper.getUnsignedInt(buf);
        }

        fileType = type;
    }
}
