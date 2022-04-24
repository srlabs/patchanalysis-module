package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.DefaultMap;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.ClassTypeDescriptor;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexFile;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.TypeDescriptor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*
    Helper class to adjust obfuscated class/field/method names

    Basic algorithm: If the name is less than 3 characters, it is likely obfuscated. So let's replace it with a
    synthetic name (based on an incrementing index). This class makes sure that these replacements are done in a
    consistent way as follows:
    * The same name will be translated to the same synthetic name within one function depending on the order of first use
    * Translations are specific to the class name for fields/methods
    * Translations are specific to the parent name (package name) for classes
 */
public class DexSignatureContext {

    Charset UTF8 = Charset.forName("UTF-8");
    DefaultMap<String, DefaultMap<String, String>> classNameAdjustments;
    DefaultMap<String, Integer> classNameAdjustmentsObfuscatedNextIndex;
    DefaultMap<String, Integer> classNameAdjustmentsInnerNextIndex;
    DefaultMap<String, DefaultMap<String, String>> fieldNameAdjustments;
    DefaultMap<String, Integer> fieldNameAdjustmentsNextIndex;
    DefaultMap<String, DefaultMap<String, String>> methodNameAdjustments;
    DefaultMap<String, Integer> methodNameAdjustmentsNextIndex;


    public DexSignatureContext() {
        classNameAdjustments = new DefaultMap<>();
        classNameAdjustmentsObfuscatedNextIndex = new DefaultMap<>();
        classNameAdjustmentsInnerNextIndex = new DefaultMap<>();
        fieldNameAdjustments = new DefaultMap<>();
        fieldNameAdjustmentsNextIndex = new DefaultMap<>();
        methodNameAdjustments = new DefaultMap<>();
        methodNameAdjustmentsNextIndex = new DefaultMap<>();
    }

    public byte[] getAdjustedClassName(DexFile dexFile, int classIndex) {
        TypeDescriptor classType = dexFile.getType(classIndex);
        if (!(classType instanceof ClassTypeDescriptor)) {
            return dexFile.getTypeDescriptorRaw(classIndex);
        }

        String className = ((ClassTypeDescriptor) classType).getClassName();
        String[] components = Helper.split(className, '.');
        List<String> componentsAdjusted = new ArrayList<>();

        for (String component : components) {
            String classNameSoFar = Helper.joinStrings(componentsAdjusted, '.');
            if (this.classNameAdjustments.getOrDefault(classNameSoFar, new DefaultMap<String, String>()).containsKey(component)) {
                component = this.classNameAdjustments.get(classNameSoFar).get(component);
            } else if (isObfuscatedName(component.getBytes(UTF8))) {
                int index = this.classNameAdjustmentsObfuscatedNextIndex.getOrDefault(classNameSoFar, 0);
                this.classNameAdjustmentsObfuscatedNextIndex.put(classNameSoFar, index + 1);
                String componentAdjusted = String.format("OBFUSCATED_%d", index);
                this.classNameAdjustments.get(classNameSoFar).put(component, componentAdjusted);
                component = componentAdjusted;
            } else if (component.contains("$")) {
                int lastIndexOfChar = (component.lastIndexOf('$') != -1) ?
                        component.lastIndexOf('$') : component.length();
                String mainClass = component.substring(0, lastIndexOfChar);
                String absoluteMainClass = String.format("%s.%s", classNameSoFar, mainClass);
                int index = this.classNameAdjustmentsInnerNextIndex.getOrDefault(absoluteMainClass, 0);
                this.classNameAdjustmentsInnerNextIndex.put(absoluteMainClass, index + 1);

                String componentAdjusted = String.format("%s$INNER_%d", mainClass, index);
                this.classNameAdjustments.get(classNameSoFar).put(component, componentAdjusted);
                component = componentAdjusted;
            }
            componentsAdjusted.add(component);
        }
        String finalAdjustedClassName = Helper.joinStrings(componentsAdjusted, '.');
        return finalAdjustedClassName.getBytes(UTF8);
    }

    public byte[] adjustFieldName(byte[] fieldName, byte[] className) {
        String fieldNameString = new String(fieldName, UTF8);
        String classNameString = new String(className, UTF8);
        if (isObfuscatedName(fieldName)) {
            if (!this.fieldNameAdjustments.getOrDefault(classNameString, new DefaultMap<String, String>()).containsKey(fieldNameString)) {
                int index = this.fieldNameAdjustmentsNextIndex.getOrDefault(classNameString, 0);
                this.fieldNameAdjustmentsNextIndex.put(classNameString, index + 1);
                String newFieldName = String.format("FIELD_%d", index);
                this.fieldNameAdjustments.get(classNameString).put(fieldNameString, newFieldName);
            }
            return this.fieldNameAdjustments.get(classNameString).get(fieldNameString).getBytes(UTF8);
        } else {
            return fieldName;
        }
    }

    public byte[] adjustMethodName(byte[] methodName, byte[] className) {
        String methodNameString = new String(methodName, UTF8);
        String classNameString = new String(className, UTF8);
        if (isObfuscatedName(methodName)) {
            if (!this.methodNameAdjustments.getOrDefault(classNameString, new DefaultMap<String, String>()).containsKey(methodNameString)) {
                int index = this.methodNameAdjustmentsNextIndex.getOrDefault(classNameString, 0);
                this.methodNameAdjustmentsNextIndex.put(classNameString, index + 1);
                String newMethodName = String.format("METHOD_%d", index);
                this.methodNameAdjustments.get(classNameString).put(methodNameString, newMethodName);
            }
            return this.methodNameAdjustments.get(classNameString).get(methodNameString).getBytes(UTF8);
        } else {
            return methodName;
        }
    }

    /*
        Tries to guess (via some heuristics) whether a given name is obfuscated. Obfuscated names will be replaced with
        an index (by order in which the name occurs within the function) in order to still get usable signatures even
        for obfuscated code.
     */
    public static boolean isObfuscatedName(byte[] name) {
        // Very short names (<= 3 characters) are often obfuscated
        List<byte[]> whitelistedNames = new ArrayList<>();
        whitelistedNames.add("app".getBytes(Charset.forName("UTF-8")));
        whitelistedNames.add("com".getBytes(Charset.forName("UTF-8")));

        for (byte[] whiteListedName : whitelistedNames) {
            if (Arrays.equals(name, whiteListedName)) {
                return false;
            }
        }
        if (name.length <= 3) {
            return true;
        }

        // No more than 2 different characters often means obfuscated, e.g. "l1ll11lll1"
        // <=> "l1ll11llll"
        Set<Byte> uniqueSetOfBytes = new HashSet<>();
        for (byte nameByte : name) {
            uniqueSetOfBytes.add(nameByte);
        }
        if (uniqueSetOfBytes.size() <= 2) {
            return true;
        }

        return false;
    }
}
