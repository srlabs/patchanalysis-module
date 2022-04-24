package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

public class TypeDescriptor {

    public static TypeDescriptor parse(String typeDescriptorStr) {
        char firstChar = typeDescriptorStr.charAt(0);
        if (firstChar == 'L') {
            return new ClassTypeDescriptor(typeDescriptorStr);
        } else if (firstChar == '[') {
            return new ArrayTypeDescriptor(typeDescriptorStr);
        } else if (BasicTypeDescriptor.basicTypes.containsKey(firstChar)) {
            return new BasicTypeDescriptor(typeDescriptorStr);
        } else {
            return null;
        }
    }
}
