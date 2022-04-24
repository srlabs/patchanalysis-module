package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class AccessFlags {

    int accessFlags;
    Map<String, Integer> enumType;

    public AccessFlags(int accessFlags, Map<String, Integer> enumType) {
        this.accessFlags = accessFlags;
        this.enumType = enumType;
    }

    public String toString() {
        int accessFlags = this.accessFlags;
        ArrayList<String> flags = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : this.enumType.entrySet()) {
            String flagName = entry.getKey();
            int flagValue = entry.getValue();
            if ((accessFlags & flagValue) == flagValue) {
                accessFlags -= flagValue;
                flags.add(flagName.toLowerCase(Locale.ROOT));
            }
        }
        return TextUtils.join(", ", flags);
    }

    public Boolean hasFlag(String flag) {
        if (!this.enumType.containsKey(flag)) {
            throw new RuntimeException("Flag is not part of enum: " + flag);
        }
        int flagValue = this.enumType.get(flag);
        return (this.accessFlags & flagValue) == flagValue;
    }
}
