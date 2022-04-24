package de.srlabs.patchanalysis_module.analysis.java_basic_tests;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

    public static byte[] open(String filePath) throws IOException {

        final FileChannel channel = new FileInputStream(filePath).getChannel();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        channel.close();
        byte[] bytesArray = new byte[buffer.remaining()];
        buffer.get(bytesArray, 0, bytesArray.length);
        return bytesArray;
    }


    public static long getUnsignedInt(ByteBuffer bb) {
        return ((long) bb.getInt() & 0xffffffffL);
    }

    public static long getUnsignedInt(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return getUnsignedInt(buf);
    }

    public static int getUnsignedShort(ByteBuffer bb) {
        return (bb.getShort() & 0xffff);
    }

    public static int getUnsignedShort(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return getUnsignedShort(buf);
    }

    public static byte[] putUnsignedInt(long value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt((int) (value & 0xffffffffL));
        return bb.array();
    }

    public static byte[] putSignedInt(int value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(value);
        return bb.array();
    }

    public static byte[] putUnsignedShort(int value) {
        ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) (value & 0xffff));
        return bb.array();
    }

    public static void printBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public static Pattern opcodePattern = Pattern.compile("\\w+$", Pattern.CASE_INSENSITIVE);
    public static Pattern classTypeDescriptorPattern = Pattern.compile("[\\w$\\-]+$", Pattern.CASE_INSENSITIVE);
    public static Pattern dexClassesPattern = Pattern.compile("classes\\d*\\.dex$", Pattern.CASE_INSENSITIVE);

    public static boolean stringFollowsRegex(String input, String regexID) {

        Matcher matcher;
        if (regexID.equals("opcode")) {
            matcher = opcodePattern.matcher(input);
        } else if (regexID.equals("classTypeDescriptor")) {
            matcher = classTypeDescriptorPattern.matcher(input);
        } else if (regexID.equals("dexClasses")) {
            matcher = dexClassesPattern.matcher(input);
        } else {
            matcher = null;
        }
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        result[0] = (byte) ((value & 0xFF000000) >> 24);
        result[1] = (byte) ((value & 0x00FF0000) >> 16);
        result[2] = (byte) ((value & 0x0000FF00) >> 8);
        result[3] = (byte) ((value & 0x000000FF) >> 0);
        return result;
    }

    public static byte[] getSlice(byte[] bytes, int start, int end) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int i = start; i < end && i < bytes.length; i++) {
            buf.write(bytes[i]);
        }

        return buf.toByteArray();
    }

    public static byte[] concatenateBytes(byte[][] entries) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] entry : entries) {
            try {
                outputStream.write(entry);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return outputStream.toByteArray();
    }

    public static ByteArrayOutputStream writeBytesToStream(ByteArrayOutputStream buf, byte[] bytes) {
        try {
            buf.write(bytes);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException("Could not write bytes to stream");
        }
    }

    // Javas toIntExact requires API Level 24 (we want to support >= 16)
    public static int longToIntExact(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new ArithmeticException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    // https://stackoverflow.com/a/45964741
    public static String[] split(final String line, final char delimiter) {
        CharSequence[] temp = new CharSequence[(line.length() / 2) + 1];
        int wordCount = 0;
        int i = 0;
        int j = line.indexOf(delimiter);

        while (j >= 0) {
            temp[wordCount++] = line.substring(i, j);
            i = j + 1;
            j = line.indexOf(delimiter, i);
        }

        temp[wordCount++] = line.substring(i);

        String[] result = new String[wordCount];
        System.arraycopy(temp, 0, result, 0, wordCount);

        return result;
    }

    public static class DefaultMap<K, V> extends ConcurrentHashMap<K, V> {

        //@Override
        public V getOrDefault(Object key, V value) {
            V returnValue = super.putIfAbsent((K) key, value);
            if (returnValue == null) {
                returnValue = value;
            }
            return returnValue;
        }
    }

    public static String joinStrings(List<String> strings, Character delimiter) {
        if (strings.size() == 0) {
            return "";
        }

        StringBuilder wordList = new StringBuilder();
        for (String s : strings) {
            wordList.append(s + delimiter);
        }
        return new String(wordList.deleteCharAt(wordList.length() - 1));
    }
}
