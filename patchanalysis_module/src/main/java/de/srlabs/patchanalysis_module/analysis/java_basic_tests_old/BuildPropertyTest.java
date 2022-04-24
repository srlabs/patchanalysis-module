package de.srlabs.patchanalysis_module.analysis.java_basic_tests_old;

import android.content.Context;
import de.srlabs.patchanalysis_module.analysis.TestUtils;

/**
 * This test class enables comparing build property values, with a given value;
 * can also be used to compare boolean values by using "0" or "1" as expectedValue
 * @author jonas
 */
public class BuildPropertyTest implements JavaBasicTest {

    private String expectedValue;
    private String buildPropertyKey;

    public BuildPropertyTest(String buildPropertyKey, String expectedValue){
        this.buildPropertyKey = buildPropertyKey;
        this.expectedValue = expectedValue;
    }

    @Override
    public Boolean runTest(Context c) throws Exception {
        if(buildPropertyKey == null || buildPropertyKey.isEmpty()){
            throw new IllegalStateException("Invalid build property key, not able to compare values.");
        }
        String currentValue = TestUtils.getBuildProperty(buildPropertyKey);
        if(currentValue != null)
          currentValue = currentValue.trim();
        else
            throw new IllegalStateException("Invalid current build property value, not able to compare values.");
        return currentValue.equals(expectedValue);
    }
}
