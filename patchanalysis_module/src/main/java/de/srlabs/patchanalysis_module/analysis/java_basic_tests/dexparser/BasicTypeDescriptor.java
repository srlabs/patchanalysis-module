package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BasicTypeDescriptor extends TypeDescriptor {

    public static final Map<Character, String> basicTypes = createStaticMap();

    private static Map<Character, String> createStaticMap() {
        Map<Character, String> result = new HashMap<>();
        result.put('V', "void");
        result.put('Z', "boolean");
        result.put('B', "byte");
        result.put('S', "short");
        result.put('C', "char");
        result.put('I', "int");
        result.put('J', "long");
        result.put('F', "float");
        result.put('D', "double");
        return Collections.unmodifiableMap(result);
    }

    String typeDescriptorStr;

    public BasicTypeDescriptor(String typeDescriptorStr) {
        if (typeDescriptorStr.length() != 1) {
            throw new RuntimeException("typeDescriptorStr has invalid length");
        }

        this.typeDescriptorStr = typeDescriptorStr;
        if (!BasicTypeDescriptor.basicTypes.containsKey(typeDescriptorStr.charAt(0))) {
            throw new RuntimeException("typeDescriptorStr has invalid value");
        }
    }

    public String toString() {
        return BasicTypeDescriptor.basicTypes.get(typeDescriptorStr.charAt(0));
    }

}
