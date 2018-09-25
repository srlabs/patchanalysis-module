// ITestExecutorServiceInterface.aidl
package de.srlabs.patchanalysis_module;

import de.srlabs.patchanalysis_module.ITestExecutorCallbacks;
import de.srlabs.patchanalysis_module.ITestExecutorDashboardCallbacks;

// Declare any non-default types here with import statements

interface ITestExecutorServiceInterface {
    void updateCallback(ITestExecutorCallbacks callback);
    void updateDashboardCallback(ITestExecutorDashboardCallbacks callback);
    boolean isAnalysisRunning();
    void requestCancelAnalysis();
}