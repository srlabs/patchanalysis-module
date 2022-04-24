package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class CodeHasher {

    ByteArrayOutputStream rawBuf;
    String signatureVersion;
    ArrayList<String> textList;

    public CodeHasher(String signatureVersion) {
        rawBuf = new ByteArrayOutputStream();
        this.signatureVersion = signatureVersion;
        textList = new ArrayList<>();
    }

    public void addBinary(byte[] buf) {
         rawBuf = Helper.writeBytesToStream(rawBuf, buf);
    }

    public void addText(String text) {
        this.textList.add(text);
    }

    public void addUInt16(int num, String name) {
        this.textList.add(name + "=" + num);
        byte[] bytes = Helper.putUnsignedShort(num);
        rawBuf = Helper.writeBytesToStream(rawBuf, bytes);
    }

    public void addUInt32(long num, String name) {
        this.textList.add(name + "=" + num);
        byte[] bytes = Helper.putUnsignedInt(num);
        rawBuf = Helper.writeBytesToStream(rawBuf, bytes);
    }

    public void addInt32(int num, String name) {
        this.textList.add(name + "=" + num);
        byte[] bytes = Helper.putSignedInt(num);
        rawBuf = Helper.writeBytesToStream(rawBuf, bytes);
    }

    public String getBinaryHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawBuf.toByteArray());
            return this.signatureVersion + ":" + Helper.bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void dump() {
        for (String debugStr : this.textList) {
             System.out.println("\t" + debugStr);
        }
    }
}
