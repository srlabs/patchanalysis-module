// ITestExecutorCallbacks.aidl
package de.srlabs.patchanalysis_module;

// Declare any non-default types here with import statements

interface ITestExecutorCallbacks {
    void updateProgress(double progressPercent);
    void showErrorMessage(String text);
    void showNoCVETestsForApiLevel(String message);
    void finished(String analysisResultString, boolean isBuildCertified, long currentAnalysisTimestamp);
    void reloadViewState();
    void handleFatalError(String stickyErrorMessage, long currentAnalysisTimestamp);
}
