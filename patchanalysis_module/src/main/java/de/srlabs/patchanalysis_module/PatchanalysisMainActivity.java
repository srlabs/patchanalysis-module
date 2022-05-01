package de.srlabs.patchanalysis_module;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.srlabs.patchanalysis_module.Constants.ActivityState;
import de.srlabs.patchanalysis_module.analysis.PatchanalysisService;
import de.srlabs.patchanalysis_module.analysis.TestUtils;
import de.srlabs.patchanalysis_module.helpers.NotificationHelper;
import de.srlabs.patchanalysis_module.helpers.ServiceConnectionHelper;
import de.srlabs.patchanalysis_module.helpers.SharedPrefsHelper;
import de.srlabs.patchanalysis_module.views.PatchanalysisSumResultChart;
import de.srlabs.patchanalysis_module.views.PatchlevelDateOverviewChart;

import static de.srlabs.patchanalysis_module.Constants.getVulnerabilityIndicatorColor;


public class PatchanalysisMainActivity extends FragmentActivity {
    private Handler handler;
    private Button startTestButton;
    private TextView errorTextView, percentageText;
    private WebView legendView;
    private ScrollView webViewContent, metaInfoTextScrollView;
    private ProgressBar progressBar;
    private LinearLayout progressBox;
    private PatchanalysisSumResultChart resultChart;

    private ITestExecutorServiceInterface mITestExecutorService;
    private NotificationHelper notificationHelper;

    private boolean isServiceBound=false;
    private boolean noInternetDialogShowing = false;
    private String currentPatchlevelDate; // Only valid in ActivityState.VULNERABILITY_LIST
    private String noCVETestsForApiLevelMessage = null;
    private static final int SDCARD_PERMISSION_RCODE = 1;
    private TestCallbacks callbacks = new TestCallbacks();
    private boolean isActivityActive = false;
    private String oldStatusMessage = null;

    private ActivityState nonPersistentState = ActivityState.PATCHLEVEL_DATES;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.callbacks = new TestCallbacks();
        Log.d(Constants.LOG_TAG, "onCreate() called");
        handler = new Handler(Looper.getMainLooper());
        setContentView(getPatchanalysisLayoutId());
        startTestButton = (Button) findViewById(getTestButtonId());
        webViewContent = (ScrollView) findViewById(getWebViewLayoutId());
        errorTextView = (TextView) findViewById(getErrorTextId());
        percentageText = (TextView) findViewById(getPercentageTextId());
        legendView = (WebView) findViewById(getLegendWebViewId());
        metaInfoTextScrollView = (ScrollView) findViewById(getMetaInfoViewId());
        progressBar = (ProgressBar) findViewById(getProgressBarId());
        resultChart = (PatchanalysisSumResultChart) findViewById(getResultChartId());
        progressBox = (LinearLayout) findViewById(getProgressBoxId());
        errorTextView.setText("");
        percentageText.setText("");
        ActionBar actionBar = getActionBar();

        // see onOptionsItemSelected
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (Constants.IS_TEST_MODE) {
            actionBar.setSubtitle("TESTMODE");
        } else {
            actionBar.setSubtitle("\nApp ID: "+ TestUtils.getAppId(AppFlavor.getAppFlavor(), this));
        }

        displayCutline(null);

        // This is not persisted right now
        if (savedInstanceState != null) {
            currentPatchlevelDate = savedInstanceState.getString("currentPatchlevelDate");
        }

        if(TestUtils.isTooOldAndroidAPIVersion()){
            startTestButton.setEnabled(false);
            progressBox.setVisibility(View.GONE);
            showMetaInformation(this.getResources().getString(R.string.patchanalysis_too_old_android_api_level),null);
        }

        notificationHelper = new NotificationHelper(this.getApplicationContext(), AppFlavor.getAppFlavor());
    }

    private void showErrorMessageInMetaInformation(String errorMessage) {
        String html = "<p style=\"font-weight:bold;\">" + this.getResources().getString(R.string.patchanalysis_sticky_error_message_start)
                + "</p>";
        showMetaInformation(html,errorMessage);
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mITestExecutorService = ITestExecutorServiceInterface.Stub.asInterface(service);
            try{
                mITestExecutorService.updateCallback(callbacks);
            } catch (RemoteException e) {
                Log.e(Constants.LOG_TAG, "RemoteException in onServiceConnected():", e);
            }
            Log.d(Constants.LOG_TAG,"Service connected!");
            isServiceBound = true;
            restoreState();
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(Constants.LOG_TAG, "Service has unexpectedly disconnected");
            isServiceBound = false;
            mITestExecutorService = null;
        }
    };

    private void setProgressBarPercent (double progressPercent) {
        progressBar.setMax(1000);
        progressBar.setProgress((int) (progressPercent * 1000.0));
        String percentageString = ""+progressPercent*100.0;
        if(percentageString.length() > 4){
            percentageString = percentageString.substring(0, 4);
            if (percentageString.endsWith(".")) {
                percentageString = percentageString.substring(0, percentageString.length() - 1);
            }
        }
        percentageText.setText(percentageString+"%");
    }

    private void showStatusMessage(String statusMessage){
        if (statusMessage != null && !statusMessage.equals(oldStatusMessage)) {
                oldStatusMessage = statusMessage;
                String explain = "<i>" + statusMessage + "</i></br>" + this.getResources().getString(R.string.patchanalysis_meta_info_analysis_in_progress);
                showMetaInformation("<b>"+this.getResources().getString(R.string.patchanalysis_sum_result_chart_analysis_in_progress)+"</b>", explain);
        }
    }

    private String getWebViewFontStyle() {
        return "<head><style type=\"text/css\">body { " +
                    "    font-family: sans-serif-condensed;\n" +
                    "    font-size: 11sp;\n" +
                    "    font-color: %2358585b;\n" +
                    "    text-align: justify;\n" +
                    "}</style></head>";
    }

    class TestCallbacks extends ITestExecutorCallbacks.Stub {
        @Override
        public void showErrorMessage(final String text) throws RemoteException {
            handler.post(new Runnable(){
                @Override
                public void run() {
                    if (isActivityActive) {
                        restoreState();
                        progressBox.setVisibility(View.INVISIBLE);
                        if(text.equals(PatchanalysisService.NO_INTERNET_CONNECTION_ERROR)){
                            showNoInternetConnectionDialog();
                        } else {
                            errorTextView.setText(text);
                        }
                    }
                }
            });
        }


        @Override
        public void updateProgress(final double progressPercent, final String statusMessage) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchanalysisMainActivity received updateProgress(" + progressPercent + ", statusMessage: "+statusMessage+")");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isActivityActive) {
                        PatchanalysisMainActivity.this.setProgressBarPercent(progressPercent);
                        if(statusMessage != null){
                                PatchanalysisMainActivity.this.showStatusMessage(statusMessage);
                        }
                    }
                    else{
                        if(statusMessage != null && !statusMessage.equals(oldStatusMessage))
                            oldStatusMessage = statusMessage;
                    }
                }
            });
        }
        @Override
        public void reloadViewState() throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchanalysisMainActivity received reloadViewState()");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isActivityActive) {
                        restoreState();
                    }
                }
            });
        }
        @Override
        public void finished(final String analysisResultString, final boolean isBuildCertified,
                             final long currentAnalysisTimestamp) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchanalysisMainActivity received finished()");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ServiceConnectionHelper.executeFinishedOncePerAnalysis(analysisResultString,
                            isBuildCertified, currentAnalysisTimestamp);
                    if (isActivityActive) {
                        restoreState();
                    }
                }
            });
        }
        @Override
        public void handleFatalError(final String stickyErrorMessage, final long currentAnalysisTimestamp) throws RemoteException {
            Log.i(Constants.LOG_TAG, "PatchanalysisMainActivity received handleFatalError()");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    startTestButton.setEnabled(false);
                    ServiceConnectionHelper.executeCancelledOncePerAnalysis(stickyErrorMessage, currentAnalysisTimestamp);
                    if (isActivityActive) {
                        restoreState();
                    }
                }
            });
        }
        @Override
        public void showNoCVETestsForApiLevel(String message) throws RemoteException{
            Log.i(Constants.LOG_TAG,"PatchanalysisMainActivity received showNoCVETestsForApiLevel()");
            noCVETestsForApiLevelMessage = message;
        }
    }
    public void showMetaInformation(String status, String explain){
        metaInfoTextScrollView.removeAllViews();
        WebView wv = new WebView(PatchanalysisMainActivity.this);
        String html = "<html>"+getWebViewFontStyle()+"<body>\n";
        if(status != null)
            html += "\t" + status;
        if(explain != null)
            html += "<br/>" + explain;
        html += "</body></html>\n";
        // Log.i(Constants.LOG_TAG,"Meta information text:\n"+html);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.loadData(html, "text/html; charset=utf-8","utf-8");
        metaInfoTextScrollView.addView(wv);
    }
    public void displayCutline(HashMap<PatchanalysisSumResultChart.Result, PatchanalysisSumResultChart.ResultPart> results){
        boolean showInconclusive = getShowInconclusivePatchAnalysisTestResults(this);
        String html = "<html>" + getWebViewFontStyle() + "<body>\n" +
                "<table style=\"border:0px collapse;float:right;\">";
        if(results == null) {
            html +=
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_PATCHED) + "\">" + this.getResources().getString(R.string.patchanalysis_patched) +
                            "</span></td><td></td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_MISSING) + "\">" + this.getResources().getString(R.string.patchanalysis_patch_missing) +
                            "</span></td><td></td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTCLAIMED) + "\">" + this.getResources().getString(R.string.patchanalysis_after_claimed_patchlevel) +
                            "</span></td><td></td></tr>";
            if(showInconclusive)
                html += "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_INCONCLUSIVE) + "\">" + this.getResources().getString(R.string.patchanalysis_inconclusive) +
                    "</span></td><td></td></tr>";

            html += "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTAFFECTED) + "\">" + this.getResources().getString(R.string.patchanalysis_not_affected) +
                            "</span></td><td></td></tr>";
        }else if((results.size() == 5 || results.size() == 4) && results.containsKey(PatchanalysisSumResultChart.Result.PATCHED) && results.containsKey(PatchanalysisSumResultChart.Result.MISSING) && results.containsKey(PatchanalysisSumResultChart.Result.NOTCLAIMED) &&
                results.containsKey(PatchanalysisSumResultChart.Result.NOTAFFECTED)){
            //display number of results for each category
            html +=
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_PATCHED) + "\">" + this.getResources().getString(R.string.patchanalysis_patched) +
                    "</span></td><td style=\"text-align:right;\">"+results.get(PatchanalysisSumResultChart.Result.PATCHED).getCount()+"</td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_MISSING) + "\">" + this.getResources().getString(R.string.patchanalysis_patch_missing) +
                    "</span></td><td style=\"text-align:right;\">"+results.get(PatchanalysisSumResultChart.Result.MISSING).getCount()+"</td></tr>" +
                    "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTCLAIMED) + "\">" + this.getResources().getString(R.string.patchanalysis_after_claimed_patchlevel) +
                    "</span></td><td style=\"text-align:right;\">"+results.get(PatchanalysisSumResultChart.Result.NOTCLAIMED).getCount()+"</td></tr>";
            if(showInconclusive)
                    html += "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_INCONCLUSIVE) + "\">" +this.getResources().getString(R.string.patchanalysis_inconclusive) +
                    "</span></td><td style=\"text-align:right;\">"+results.get(PatchanalysisSumResultChart.Result.INCONCLUSIVE).getCount()+"</td></tr>";
            html += "\t<tr><td style=\"padding-right:10px\"><span style=\"color:" + toColorString(Constants.COLOR_NOTAFFECTED) + "\">" + this.getResources().getString(R.string.patchanalysis_not_affected) +
                    "</span></td><td style=\"text-align:right;\">"+results.get(PatchanalysisSumResultChart.Result.NOTAFFECTED).getCount()+"</td></tr>";
        }
        else{
            Log.e(Constants.LOG_TAG,"displayCutline: Result information missing!");
        }

        html += "</table>\n" +
                "</body></html>";
        legendView.setBackgroundColor(Color.TRANSPARENT);
        legendView.loadUrl("about:blank");
        legendView.loadDataWithBaseURL(Constants.WEBVIEW_URL_LOADDATA, html,"text/html; charset=utf-8","utf-8", null);
    }


    @Override
    protected void onResume(){
        super.onResume();
        isActivityActive = true;
        if(!TestUtils.isTooOldAndroidAPIVersion()) {
            Intent intent = new Intent(PatchanalysisMainActivity.this, PatchanalysisService.class);
            intent.setAction(ITestExecutorServiceInterface.class.getName());
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        restoreState();
    }


    private void startServiceIfNotRunning(){
        try {
            if (mITestExecutorService == null || !mITestExecutorService.isAnalysisRunning()) {
                Intent intent = new Intent(PatchanalysisMainActivity.this, PatchanalysisService.class);
                intent.setAction(ITestExecutorServiceInterface.class.getName());
                startService(intent);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }
        } catch (RemoteException e) {
            Log.d(Constants.LOG_TAG,"RemoteException in startServiceIfNotRunning" , e);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        notificationHelper.cancelNonStickyNotifications();
        isActivityActive = false;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mConnection != null){
            unbindService(mConnection);
        }
        SharedPrefsHelper.clearSavedStickyErrorMessage(PatchanalysisMainActivity.this);
    }


    private synchronized void restoreState(){
        notificationHelper.cancelNonStickyNotifications();
        ActivityState tempNonPersistentState = nonPersistentState;
        try {
            if (mITestExecutorService != null && mITestExecutorService.isAnalysisRunning()) {
                // Analysis is running, show progress bar
                setButtonCancelAnalysis();
                progressBox.setVisibility(View.VISIBLE);
                resultChart.setVisibility(View.GONE);
                resultChart.setAnalysisRunning(true);
                webViewContent.setVisibility(View.INVISIBLE);
                displayCutline(null);
                if(oldStatusMessage != null) {
                    showStatusMessage(oldStatusMessage);
                }else {
                    showMetaInformation(this.getResources().getString(R.string.patchanalysis_analysis_in_progress), this.getResources().getString(R.string.patchanalysis_meta_info_analysis_in_progress));
                }
            } else {
                // Analysis is not running
                resultChart.setAnalysisRunning(false);
                progressBox.setVisibility(View.GONE);
                if (SharedPrefsHelper.getAnalysisResult(this) == null) {
                    // No analysis result available
                    resultChart.setVisibility(View.GONE);
                    webViewContent.setVisibility(View.INVISIBLE);
                    String stickyErrorMessage = SharedPrefsHelper.getStickyErrorMessage(this);
                    displayCutline(null);
                    if (stickyErrorMessage != null) {
                        // Last analysis failed recently
                        PatchanalysisMainActivity.this.showErrorMessageInMetaInformation(stickyErrorMessage);
                    } else {
                        // No analysis executed yet, show no error message
                        showMetaInformation(this.getResources().getString(R.string.patchanalysis_claimed_patchlevel_date)+": "
                                + TestUtils.getPatchlevelDate(),this.getResources().getString(R.string.patchanalysis_no_results_yet));
                    }

                } else {
                    // Previous analysis result available, show results table
                    resultChart.loadValuesFromCachedResult(this);
                    if (resultChart.hasCountedCategories()) {
                        // Counted categories available, show bar chart
                        resultChart.setVisibility(View.VISIBLE);
                        resultChart.invalidate();
                    }
                    webViewContent.setVisibility(View.VISIBLE);
                    showPatchlevelDateNoTable();//should also update the cutline info
                }
                setButtonStartAnalysis();
            }
        } catch (RemoteException e) {
            Log.d(Constants.LOG_TAG,"RemoteException in restoreState" , e);
        }
        if(tempNonPersistentState == ActivityState.VULNERABILITY_LIST) {
            // show vulnerability details for a specific category
            showDetailsNoTable(currentPatchlevelDate);
        }
    }

    private void setButtonStartAnalysis() {
        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTestButton.setEnabled(false);
                startTest();
            }
        });
        startTestButton.setText(this.getResources().getString(R.string.patchanalysis_button_start_analysis));
        startTestButton.setEnabled(true);
    }

    private void setButtonCancelAnalysis() {
        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerCancelAnalysis();
            }
        });
        startTestButton.setText(this.getResources().getString(R.string.patchanalysis_button_cancel_analysis));
        startTestButton.setEnabled(true);
    }

    private void triggerCancelAnalysis() {
        startTestButton.setEnabled(false);
        resultChart.setAnalysisRunning(false);
        ITestExecutorServiceInterface temp = PatchanalysisMainActivity.this.mITestExecutorService;
        try {
            if (temp != null && temp.isAnalysisRunning()) {
                temp.requestCancelAnalysis();
            }
        } catch (RemoteException e) {
            Log.e(Constants.LOG_TAG, "RemoteException in triggerCancelAnalysis:", e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("currentPatchlevelDate", currentPatchlevelDate);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void startTest(){
        if(!TestUtils.isTooOldAndroidAPIVersion()) {
            SharedPrefsHelper.clearSavedAnalysisResultNonPersistent();
            SharedPrefsHelper.clearSavedStickyErrorMessageNonPersistent();
            resultChart.resetCounts();
            PatchanalysisSumResultChart.setResultToDrawFromOnNextUpdate(null);
            resultChart.invalidate();
            setProgressBarPercent(0);

            if (TestUtils.isConnectedToInternet(this)) {
                noCVETestsForApiLevelMessage = null;
                clearTable();
                if (Constants.IS_TEST_MODE && !requestSdcardPermission()) {
                    return;
                }
                startServiceIfNotRunning();
                resultChart.setAnalysisRunning(true);
                // restoreState should be called via callback
            } else {
                //no internet connection
                Log.w(Constants.LOG_TAG, "Not testing, because of missing internet connection.");
                showNoInternetConnectionDialog();
                restoreState();
            }
        }
    }

    private boolean requestSdcardPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                SDCARD_PERMISSION_RCODE);
        return false;
    }
    private void clearTable(){
        webViewContent.removeAllViews();
    }



    private void showPatchlevelDateNoTable(){
        Log.d(Constants.LOG_TAG, "Showing optional CVEs: "+getShowOptionalCVES(this));
        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable()");
        webViewContent.removeAllViews();
        //Log.i(Constants.LOG_TAG, "showPatchlevelDateNoTable(): w=" + webViewContent.getWidth() + "  h=" + webViewContent.getHeight() + "  innerW=" + webViewContent.getChildAt(0).getWidth() + "  innerH=" + webViewContent.getChildAt(0).getHeight());
        try{
            JSONObject testResults = SharedPrefsHelper.getAnalysisResult(this);
            String metaInfo = this.getResources().getString(R.string.patchanalysis_claimed_patchlevel_date)+": <b>" + refPatchlevelDate +"</b>";
            if(SharedPrefsHelper.getAnalysisResult(this) == null) {
                showMetaInformation(metaInfo,null);
                displayCutline(null);
                return;
            }
            if (SharedPrefsHelper.isBuildFromLastAnalysisCertified(this)) {
                metaInfo += " " + this.getResources().getString(R.string.patchanalysis_certified_build);
            }
            showMetaInformation(metaInfo,null);
            Vector<String> categories = new Vector<String>();
            Iterator<String> categoryIterator = testResults.keys();
            while (categoryIterator.hasNext())
                categories.add(categoryIterator.next());
            Collections.sort(categories);
            LinearLayout rows = new LinearLayout(this);
            rows.setOrientation(LinearLayout.VERTICAL);
            rows.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));


            for (final String category : categories) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                Button button = (Button) getLayoutInflater().inflate(getCustomButtonLayoutId(), null);
                String truncatedCategory = category;
                if (category.startsWith("201")) {
                    truncatedCategory = category.substring(0, 7);
                }
                else if(category.equals("other")){
                    truncatedCategory = "General";
                }
                button.setText(truncatedCategory);
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDetailsNoTable(category);
                    }
                });

                row.addView(button);
                JSONArray vulnerabilitiesForCategory = testResults.getJSONArray(category);

                Vector<Integer> statusColors = new Vector<Integer>();

                for (int i = 0; i < vulnerabilitiesForCategory.length(); i++) {
                    JSONObject vulnerability = vulnerabilitiesForCategory.getJSONObject(i);

                    int color = getVulnerabilityIndicatorColor(vulnerability, category);

                    if(!getShowInconclusivePatchAnalysisTestResults(this) && (color == Constants.COLOR_INCONCLUSIVE)) //only show inconclusive in PatchLevelDateOverviewChart if enabled in settings
                        continue;

                    if(vulnerability.has("optional") && vulnerability.getBoolean("optional") && !getShowOptionalCVES(this))
                        continue;

                    statusColors.add(color);
                }

                //Do not show empty categories (rows)
                if(statusColors.size() == 0){
                    continue;
                }

                int[] tmp = new int[statusColors.size()];
                for (int i = 0; i < statusColors.size(); i++) {
                    tmp[i] = statusColors.get(i);
                }
                PatchlevelDateOverviewChart chart = new PatchlevelDateOverviewChart(this, tmp);
                chart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDetailsNoTable(category);
                    }
                });
                row.addView(chart);
                rows.addView(row);
            }

            webViewContent.addView(rows);
            nonPersistentState = ActivityState.PATCHLEVEL_DATES;

            if(noCVETestsForApiLevelMessage != null){
                metaInfo += getNoCVETestsForApiLevelExtraMetaInfo(noCVETestsForApiLevelMessage);
            }
            showMetaInformation(metaInfo,null);
            progressBox.setVisibility(View.GONE);

            //update counts in cutline
            displayCutline(resultChart.getParts());

        } catch(Exception e){
            Log.e(Constants.LOG_TAG, "showPatchlevelDateTable Exception", e);
        }
    }

    private void showDetailsNoTable(String category) {

        String refPatchlevelDate = TestUtils.getPatchlevelDate();
        Log.i(Constants.LOG_TAG, "refPatchlevelDate=" + refPatchlevelDate);
        int numAffectedVulnerabilities = 0;
        try {
            JSONObject testResults = SharedPrefsHelper.getAnalysisResult(this);
            if (testResults == null) {
                showMetaInformation(this.getResources().getString(R.string.patchanalysis_claimed_patchlevel_date) + ": " + refPatchlevelDate, this.getResources().getString(R.string.patchanalysis_no_test_result) + "!");
                return;
            }
            JSONArray vulnerabilitiesForPatchlevelDate = testResults.getJSONArray(category);

            WebView wv = new WebView(PatchanalysisMainActivity.this);
            StringBuilder html = new StringBuilder();
            html.append("<html>" + getWebViewFontStyle() + "<body><table style='border-collapse:collapse;'>\n");

            for (int i = 0; i < vulnerabilitiesForPatchlevelDate.length(); i++) {
                JSONObject vulnerability = vulnerabilitiesForPatchlevelDate.getJSONObject(i);
                int resultColor = getVulnerabilityIndicatorColor(vulnerability, category);

                if (!getShowInconclusivePatchAnalysisTestResults(this) && resultColor == Constants.COLOR_INCONCLUSIVE) // do not show and count inconclusive results, if disabled in settings
                    continue;

                if (vulnerability.has("optional") && vulnerability.getBoolean("optional") && !getShowOptionalCVES(this))
                    continue;

                String identifier = vulnerability.getString("identifier");
                String identifierColor = toColorString(resultColor);
                identifierColor = identifierColor.replace("#", "");
                String description = vulnerability.getString("title");
                html.append("<tr>");

                if(category.equals("other")){
                    html.append("<td>");
                    String style = "background-color: " + identifierColor + "; white-space: nowrap;" +
                            "margin: 5px 0; padding: 5px;";
                    html.append(String.format("<p style=%s>", style));
                }
                else{
                    html.append("<td style='border-bottom: 1px solid rgb(221, 221, 221);'>");
                    String style = "'background-color: " + identifierColor + "; white-space: nowrap;" +
                            "margin-right: 10px; padding: 5px;'";
                    html.append(String.format("<p style=%s>", style));
                }

                html.append(identifier);
                html.append("</p></td>");
                html.append("<td style='border-bottom: 1px solid rgb(221, 221, 221);padding:5px 0;'>");
                html.append("<p>");
                html.append(description);
                html.append("</p></td>");
                html.append("</tr>");
                numAffectedVulnerabilities++;
            }
            html.append("</table></body></html>");

            wv.setBackgroundColor(Color.TRANSPARENT);
            wv.loadData(html.toString(), "text/html; charset=utf-8", "utf-8");
            webViewContent.removeAllViews();
            webViewContent.addView(wv);

            showCategoryMetaInfo(category, numAffectedVulnerabilities);

            nonPersistentState = ActivityState.VULNERABILITY_LIST;
            currentPatchlevelDate = category;
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "showDetailsNoTable Exception", e);
        }
    }

    private void showCategoryMetaInfo(String category, int numCVEs) {
        StringBuilder infoText = new StringBuilder();
        if(!category.equals("other")) {
            infoText.append("<span style=\"font-weight:bold;\">" + category);
            infoText.append("</span><span>: " + numCVEs + " CVEs total</span>");
        }else{
            infoText.append("<span style=\"font-weight:bold;\">"+this.getResources().getString(R.string.patchanalysis_general_tests));
            infoText.append("</span><span>: " + numCVEs + " tests total</span>");
        }
        showMetaInformation(infoText.toString(),null);
    }


    private String toColorString(int color){
        String hexColorString = Integer.toHexString(color).toUpperCase();
        if(hexColorString.length() == 8){
            hexColorString = hexColorString.substring(2,8);
        }
        return "#"+hexColorString;
    }
    @Override
    public void onBackPressed(){
        if(nonPersistentState == ActivityState.VULNERABILITY_LIST)
            showPatchlevelDateNoTable();
        else
            super.onBackPressed();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        restoreState();
    }
    public void showNoInternetConnectionDialog(){
        if(isActivityActive && !noInternetDialogShowing) {
            Log.d(Constants.LOG_TAG,"Showing internet connection issues dialog");
            showMetaInformation("",null);

            AlertDialog.Builder builder = new AlertDialog.Builder(PatchanalysisMainActivity.this);

            builder.setTitle(this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_title));
            builder.setMessage(this.getResources().getString(R.string.patchanalysis_dialog_no_internet_connection_text));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    noInternetDialogShowing = false;
                }
            });
            builder.setOnCancelListener(null);
            builder.setCancelable(false);

            Dialog noInternetConnectionDialog = builder.create();
            noInternetConnectionDialog.show();
            noInternetDialogShowing = true;
        }
    }

    private String getNoCVETestsForApiLevelExtraMetaInfo(String message){
        StringBuilder information = new StringBuilder();
        information.append("</br><b><u>"+this.getResources().getString(R.string.patchanalysis_dialog_note_title)+"</u></b></br>");
        information.append(message+"</br>");
        if (Build.VERSION.SDK_INT == 32) {
            information.append("Android OS version: 12L");
        } else {
            information.append("Android OS version: "+ Build.VERSION.RELEASE);
        }
        return information.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(Constants.LOG_TAG, "Received permission request result; code: " + requestCode);
        if (requestCode == SDCARD_PERMISSION_RCODE) {
            Log.d(Constants.LOG_TAG,"Received request permission results for external storage...");
            if (grantResults.length > 0) {
                //find all neccessary permissions not granted
                List<String> notGrantedPermissions = new LinkedList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        notGrantedPermissions.add(permissions[i]);
                    }
                }

                if (notGrantedPermissions.isEmpty()) {
                    //Success: All neccessary permissions granted
                    // start test in TEST MODE!
                    startServiceIfNotRunning();
                } else {
                    //ask again for all not granted permissions
                    boolean showDialog = false;
                    for (String notGrantedPermission : notGrantedPermissions) {
                        showDialog = showDialog || ActivityCompat.shouldShowRequestPermissionRationale(this, notGrantedPermission);
                    }
                    if(showDialog){ //ask for permission in a loop, cause otherwise test mode will not work
                        requestSdcardPermission();
                    }
                }

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(getPatchAnalysisMenuId(), menu);

        resetMenuEntryStates(menu);

        return true;
    }

    private void resetMenuEntryStates(Menu menu) {
        if(menu != null) {
            //set "show inconclusive results" checkbox to current state
            MenuItem showInconclusiveMenuItem = menu.findItem(getShowInconclusiveMenuItemId());
            showInconclusiveMenuItem.setChecked(getShowInconclusivePatchAnalysisTestResults(this));
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if(item.getItemId() ==  android.R.id.home) {
            AppFlavor.getAppFlavor().homeUpButtonMainActivitiyCallback(this, item);
            return true;
        }
        else if(item.getItemId() == getShowInconclusiveMenuItemId()) {
            boolean showInconclusive = !item.isChecked();
            item.setChecked(showInconclusive);
            setShowInconclusiveResults(this, showInconclusive);
            restoreState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setShowInconclusiveResults(Context context, boolean showInconclusive) {
        AppFlavor.getAppFlavor().setShowInconclusiveResults(context, showInconclusive);
    }
    private boolean getShowInconclusivePatchAnalysisTestResults(Context context) {
        return AppFlavor.getAppFlavor().getShowInconclusivePatchAnalysisTestResults(context);
    }

    /*private void setShowOptionalCVEs(Context context, boolean showOptionalCVEs) {
        AppFlavor.getAppFlavor().setShowOptionalCVEs(context, showOptionalCVEs);
    }*/

    public int getShowInconclusiveMenuItemId() { return R.id.menu_action_pa_inconclusive; }

    private boolean getShowOptionalCVES(Context context) {
        return AppFlavor.getAppFlavor().getShowOptionalCVES(context);
    }

    public int getPatchAnalysisMenuId() {
        return R.menu.patch_analysis;
    }

    public int getPatchanalysisLayoutId() {
        return R.layout.activity_patchanalysis;
    }

    public int getCustomButtonLayoutId() {
        return R.layout.custom_button;
    }

    public int getTestButtonId() {
        return R.id.btnDoIt;
    }

    public int getWebViewLayoutId() {
        return R.id.scrollViewTable;
    }

    public int getErrorTextId() {
        return R.id.errorText;
    }

    public int getPercentageTextId() {
        return R.id.textPercentage;
    }

    public int getLegendWebViewId() {
        return R.id.legend;
    }

    public int getMetaInfoViewId() {
        return R.id.scrollViewText;
    }

    public int getProgressBarId() {
        return R.id.progressBar;
    }

    public int getResultChartId() {
        return R.id.sumResultChart;
    }

    public int getProgressBoxId() {
        return R.id.progress_box;
    }

}
