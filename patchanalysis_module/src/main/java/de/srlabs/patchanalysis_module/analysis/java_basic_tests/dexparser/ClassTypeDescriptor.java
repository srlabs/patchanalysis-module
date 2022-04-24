package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.split;

public class ClassTypeDescriptor extends TypeDescriptor {

    String typeDescriptorStr;
    String fullClassName;
    String className;

    public ClassTypeDescriptor(String typeDescriptorStr) {
        this.typeDescriptorStr = typeDescriptorStr;

        if (typeDescriptorStr.startsWith("L") && typeDescriptorStr.endsWith(";")) {
            fullClassName = typeDescriptorStr.substring(1, typeDescriptorStr.length() - 1);
            String[] fullClassNameSplit = split(fullClassName, '/');
            className = fullClassNameSplit[fullClassNameSplit.length - 1];
            if (!Helper.stringFollowsRegex(className, "classTypeDescriptor")) {
                throw new RuntimeException("Bad class name: " + className);
            }
        }
    }

    public String toString() {
        return getClassName();
    }

    public String getClassName() {
        return fullClassName.replace('/', '.');
    }
}
