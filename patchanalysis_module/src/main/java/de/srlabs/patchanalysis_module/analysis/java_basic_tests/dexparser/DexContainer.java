package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import java.util.ArrayList;

public class DexContainer {

    public DexContainer() {

    }

    public ArrayList<DexFile> getDexFiles() {
        throw new RuntimeException("This method needs to be implemented by children");
    }

    public DexClass[] getDexClass(String className) {

        ArrayList<DexClass> result = new ArrayList<>();
        for (DexFile dex : this.getDexFiles()) {
            if (dex.hasDexClass(className)) {
                result.add(dex.getDexClass(className));
            }
        }
        return result.toArray(new DexClass[0]);
    }
}
