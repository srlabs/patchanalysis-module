package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AccessFlagsField extends AccessFlags {

    public static final Map<String, Integer> fieldTypes = createStaticMap();

    private static Map<String, Integer> createStaticMap() {
        Map<String, Integer> result = new HashMap<>();
        result.put("PUBLIC", 0x1);
        result.put("PRIVATE", 0x2);
        result.put("PROTECTED", 0x4);
        result.put("STATIC", 0x8);
        result.put("FINAL", 0x10);
        result.put("SYNCHRONIZED", 0x20);
        result.put("VOLATILE", 0x40);
        result.put("TRANSIENT", 0x80);
        result.put("NATIVE", 0x100);
        result.put("INTERFACE", 0x200);
        result.put("ABSTRACT", 0x400);
        result.put("STRICT", 0x800);
        result.put("SYNTHETIC", 0x1000);
        result.put("ANNOTATION", 0x2000);
        result.put("ENUM", 0x4000);
        result.put("UNUSED", 0x8000);
        result.put("CONSTRUCTOR", 0x10000);
        result.put("DECLARED_SYNCHRONIZED", 0x20000);
        return Collections.unmodifiableMap(result);
    }

    public AccessFlagsField(int accessFlags) {
        super(accessFlags, fieldTypes);
    }
}
