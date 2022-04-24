package de.srlabs.patchanalysis_module.analysis;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexClass;
import de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser.DexContainer;
import de.srlabs.patchanalysis_module.analysis.signatures.SymbolInformation;

/**This class bundles similar tests (== same file and probably same test type) together to cache certain information.
 * This way we can reduce the amount of redundant work like e.g. the same objdump calls.
 * Created by jonas on 15.03.18.
 */

public class TestBundle {

    private Vector<JSONObject> basicTests = null;
    private Vector<String> objdumpLines = null;
    private HashMap<String, SymbolInformation> symbolTable = null;
    private String filename = null;
    private boolean isStopMarker = false;
    private boolean targetFileExists = true;
    public DexCache dexCache = new DexCache();

    public class DexCache {
        private DexContainer container = null;
        private Boolean isValid = null;
        private Boolean fileHasBeenProcessed = false;

        public void setIsValid(Boolean isValid) {
            this.isValid = isValid;
        }
        public Boolean getIsValid() {
            return isValid;
        }
        public void setContainer(DexContainer container) {
            this.container = container;
        }
        public DexContainer getContainer() {
            return container;
        }
        public void setFileHasBeenProcessed(Boolean fileHasBeenProcessed) {
           this.fileHasBeenProcessed = fileHasBeenProcessed;
        }
        public Boolean getFileHasBeenProcessed() {
            return fileHasBeenProcessed;
        }
    }

    private TestBundle(){
        this.basicTests = new Vector<JSONObject>();
    }

    public TestBundle(String filename){
        this.filename = filename;
        this.basicTests = new Vector<JSONObject>();
    }

    public static TestBundle getStopMarker(){
        TestBundle stopMarker = new TestBundle();
        stopMarker.setStopMarker();
        return stopMarker;
    }

    public void add(JSONObject basicTest){
        basicTests.add(basicTest);
    }

    public void setSymbolTable(HashMap<String, SymbolInformation> symbolTable){
        this.symbolTable = symbolTable;
    }

    public void setStopMarker() {
        this.isStopMarker = true;
    }

    public boolean isStopMarker() {
        return isStopMarker;
    }

    public Vector<JSONObject> getBasicTests() {
        return basicTests;
    }

    public String getFilename(){
        return filename;
    }

    public HashMap<String,SymbolInformation> getSymbolTable() {
        return symbolTable;
    }

    public int getTestCount(){
        return basicTests.size();
    }

    public Vector<String> getObjdumpLines() {
        return objdumpLines;
    }

    public void setObjdumpLines(Vector<String> objdumpLines) {
        this.objdumpLines = objdumpLines;
    }

    public void checkTargetFileExists(){
        File targetFile = new File(filename);
        if (!targetFile.exists()) {
            targetFileExists = false;
        }
    }

    public boolean isTargetFileExisting(){
        return targetFileExists;
    }
}
