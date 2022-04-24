package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import android.text.TextUtils;

import java.util.Collections;

public class ArrayTypeDescriptor extends TypeDescriptor {

    String typeDescriptorStr;
    int dimension;
    TypeDescriptor elementType;

    public ArrayTypeDescriptor(String typeDescriptorStr) {
        this.typeDescriptorStr = typeDescriptorStr;
        dimension = 0;
        while (typeDescriptorStr.charAt(dimension) == '[') {
            dimension += 1;
        }
        elementType = parse(typeDescriptorStr.substring(dimension));
    }

    public String toString() {
        return String.format("%s%s", this.elementType,
                TextUtils.join("", Collections.nCopies(this.dimension, "[]")));
    }
}
