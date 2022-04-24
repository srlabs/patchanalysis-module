package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class TypedContainer {

    public String toString() {
        Field[] fields = this.getClass().getDeclaredFields();
        ArrayList<String> classData = new ArrayList<>();
        classData.add(this.getClass().getSimpleName());
        for (Field field : fields) {
            try {
                classData.add(field.getName() + " = " + field.get(this));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return TextUtils.join("\n\t", classData);
    }
}
