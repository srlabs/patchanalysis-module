package de.srlabs.patchanalysis_module.helpers.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import de.srlabs.patchanalysis_module.Constants;
import de.srlabs.patchanalysis_module.util.BasicTestParser;
import de.srlabs.patchanalysis_module.util.FoundInvalidBasicTestException;

/**This class acts as a helper to query the SQLite DB
 * Created by jonas on 04.04.18.
 */

public class DBHelper {

    private SQLiteDatabase db;
    private static final int DB_LOCKED_RETRIES = 3;
    private static final String mTAG = "DBHelper: ";
    private PADatabaseManager paDatabaseManager;


    public DBHelper(Context context){
        PADatabaseManager.initializeInstance(new PASQLiteOpenHelper(context));
        paDatabaseManager = PADatabaseManager.getInstance();
        if(db == null || !db.isOpen())
            db = paDatabaseManager.openDatabase();
    }

    public void closeDB(){
        if(paDatabaseManager != null)
            paDatabaseManager.closeDatabase();
    }

    /**
     * Write a JSONObject representation of a basic test to the SQLite DB
     * @param basicTest
     * @return
     */
    public void insertBasicTestToDB(final JSONObject basicTest) throws IllegalStateException, SQLiteConstraintException, FoundInvalidBasicTestException {
        if(basicTest == null ){
            throw new IllegalStateException(mTAG+"Null objects can not be added to DB, sneaky...");
        }
        if(!basicTest.has("uuid") || !basicTest.has("testType")){
            throw new IllegalStateException("JSONObject does not contain necessary info (uuid + testType): "+basicTest.toString());
        }

        try {
            if(db == null || !db.isOpen())
                db = PADatabaseManager.getInstance().openDatabase();
            //get all keys from basic test
            Iterator<String> keyIterator = basicTest.keys();
            Set<String> keys = new HashSet<String>();
            while (keyIterator.hasNext()) {
                keys.add(keyIterator.next());
            }

            BasicTestParser.checkTestTypeSufficientInfo(basicTest);

            ContentValues values = new ContentValues();
            //write to basictests table
            for (String key : keys) {
                if (key.equals("substring")) {
                    byte[] data = basicTest.getString("substring").getBytes("UTF-8");
                    String base64String = Base64.encodeToString(data, Base64.DEFAULT);
                    values.put("substringB64", base64String);
                } else {
                    values.put(key, basicTest.getString(key));
                }
            }

            try {
                db.insertOrThrow("basictests", null, values);
            }catch(SQLiteDatabaseLockedException e){
                Log.d(Constants.LOG_TAG,"insertBasicTestToDB: DB is locked...retrying..");
                for(int i= 0;i < DB_LOCKED_RETRIES; i++){
                    try{
                        db.insertOrThrow("basictests", null, values);
                        break;
                    }
                    catch(SQLiteDatabaseLockedException e2){
                        Log.d(Constants.LOG_TAG,"insertBasicTestToDB: DB is still locked... trying again.");
                    }
                }
            }

        }catch(JSONException | UnsupportedEncodingException e){
            Log.e(Constants.LOG_TAG,mTAG+"Error while parsing JSON info and adding basic test to DB:"+e);
        }
    }

    public void addTestResultToDB(String uuid, Boolean result){
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabase();
        }
        ContentValues values = new ContentValues();
        //write to basictests table
        if(result == null)
            values.put("result",2);
        else if(result)
            values.put("result",1);
        else
            values.put("result",0);

        try {
            db.update("basictests",values,"uuid = ?",new String[]{uuid});
        }catch(SQLiteDatabaseLockedException e){
            Log.d(Constants.LOG_TAG,"addTestResultToDB: DB is locked...retrying.");
            for(int i= 0;i < DB_LOCKED_RETRIES; i++){
                try{
                    db.update("basictests",values,"uuid = ?",new String[]{uuid});
                    break;
                }
                catch(SQLiteDatabaseLockedException e2){
                    Log.d(Constants.LOG_TAG,"addTestResultToDB: DB is still locked... trying again.");
                }
            }
        }
    }

    public void addTestExceptionToDB(String uuid, String exception){
        if(exception == null)
            return;
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabase();
        }
        ContentValues values = new ContentValues();
        //write to basictests table
        values.put("exception",exception);
        try {
            db.update("basictests", values, "uuid = ?", new String[]{uuid});
        }catch(SQLiteDatabaseLockedException e){
            Log.d(Constants.LOG_TAG,"addTestExceptionToDB: DB is locked...retrying.");
            for(int i= 0;i < DB_LOCKED_RETRIES; i++){
                try{
                    db.update("basictests", values, "uuid = ?", new String[]{uuid});
                    break;
                }
                catch(SQLiteDatabaseLockedException e2){
                    Log.d(Constants.LOG_TAG,"addTestExceptionToDB: DB is still locked... trying again.");
                }
            }
        }
    }

    public Vector<JSONObject> getNotPerformedTests(int limit){
        //Log.d(Constants.LOG_TAG,"getNotPerformedTests called with limit: "+limit);
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabase();
        }

        Vector<JSONObject> results = new Vector<>();

        //basic test table info
        Cursor cursor = db.query(
                "basictests",
                null,
                "result = ? and exception IS NULL",
                new String[]{"-1"},
                null,
                null,
                null,
                ""+limit
        );

        Log.d(Constants.LOG_TAG,"Got batch of tests with size: "+cursor.getCount()+" from DB!");

        while(cursor.moveToNext()){
            try {
                JSONObject basicTest = null;
                basicTest = new JSONObject();
                String[] columns = cursor.getColumnNames();
                String testType = null;
                for (String column : columns) {
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                results.add(basicTest);
            }catch(JSONException e){
                Log.d(Constants.LOG_TAG,"JSONException while parsing basic test:"+e.getMessage());
            }
        }
        cursor.close();
        return results;
    }

    public Vector<JSONObject> getNotPerformedTestsSortedByFilenameAndTestType( int limit){
        //Log.d(Constants.LOG_TAG,"getNotPerformedTests called with limit: "+limit);
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabaseReadOnly();
        }

        Vector<JSONObject> results = new Vector<>();

        //basic test table info
        Cursor cursor = db.query(
                "basictests",
                null,
                "result = ? and exception IS NULL",
                new String[]{"-1"},
                null,
                null,
                "filename, testType DESC",
                ""+limit
        );

        if(cursor == null || cursor.getCount() == 0)
            return null;

        Log.d(Constants.LOG_TAG,"Got batch of tests with size: "+cursor.getCount()+" from DB!");

        while(cursor.moveToNext()){
            try {
                JSONObject basicTest = null;
                basicTest = new JSONObject();
                String[] columns = cursor.getColumnNames();
                for (String column : columns) {
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                results.add(basicTest);
            }catch(JSONException e){
                Log.d(Constants.LOG_TAG,"JSONException while parsing basic test:"+e.getMessage());
            }
        }
        cursor.close();
        return results;
    }

    public Boolean getTestResult(String uuid){
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabaseReadOnly();
        }
        //basic test table info
        Cursor cursor = db.query(
                "basictests",
                new String[]{"result"},
                "uuid = ?",
                new String[]{uuid},
                null,
                null,
                null
        );
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndex("result"));
        cursor.close();

        if(result == 2 || result == -1) //inconclusive or not performed yet
            return null;
        return (result == 1);
    }


    public Vector<String> getAllBasicTestsUUIDs() throws JSONException {
        //make sure DB access is ready (read only)
        if (db == null || !db.isOpen()) {
            db = PADatabaseManager.getInstance().openDatabaseReadOnly();
        }

        //basic test table info
        Cursor cursor = db.query(
                "basictests",
                new String[]{"uuid"},
                null,
                null,
                null,
                null,
                null
        );

        Log.d(Constants.LOG_TAG, mTAG + "" + cursor.getCount() + " basic tests in DB");
        if(cursor.getCount() > 0) {
            Vector<String> uuids = new Vector<String>();
            int uuidIndex = cursor.getColumnIndex("uuid");
            while (cursor.moveToNext()) {
                String uuid = cursor.getString(uuidIndex);
                uuids.add(uuid);
            }
            cursor.close();
            return uuids;
        }
        cursor.close();
        return null;
    }

    public Vector<JSONObject> getAllBasicTests() throws JSONException, UnsupportedEncodingException {
        //make sure DB access is ready (read only)
        if (db == null || !db.isOpen()) {
            db = PADatabaseManager.getInstance().openDatabaseReadOnly();
        }

        //basic test table info
        Cursor cursor = db.query(
                "basictests",
                null,
                null,
                null,
                null,
                null,
                null
        );

        Log.d(Constants.LOG_TAG, mTAG + "" + cursor.getCount() + " basic tests in DB");
        if(cursor.getCount() > 0) {
            Vector<JSONObject> basicTests = new Vector<JSONObject>();
            while (cursor.moveToNext()) {
                JSONObject basicTest = null;
                String[] columns = cursor.getColumnNames();
                String testType = null;
                for (String column : columns) {
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                basicTests.add(basicTest);
            }
            cursor.close();
            return basicTests;
        }
        cursor.close();
        return null;
    }

    public JSONObject getBasicTestByUUID(String uuid){
        if(uuid == null || uuid.equals("")){
            Log.e(Constants.LOG_TAG,mTAG+"Malformated UUID.");
        }
        //make sure DB access is ready (read only)
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabaseReadOnly();
        }
        Cursor cursor = null;
        try {
            //basic test table info
            cursor = db.query(
                    "basictests",
                    null,
                    "uuid = ?",
                    new String[]{uuid},
                    null,
                    null,
                    null
            );

            //Log.d(Constants.LOG_TAG,"DB Query: cursor: "+cursor.getCount()+" columns:"+ Arrays.toString(cursor.getColumnNames()));
            JSONObject basicTest = null;
            if(cursor.moveToFirst()) {
                basicTest = new JSONObject();
                basicTest.put("uuid", uuid);
                String[] columns = cursor.getColumnNames();
                String testType = null;
                for (String column : columns) {
                    if (column.equals("uuid"))
                        continue;
                    if (column.equals("result")) {
                        int result = cursor.getInt(cursor.getColumnIndex("result"));
                        if (result == 2 || result == -1) //inconclusive or not performed yet
                            basicTest.put("result", JSONObject.NULL);
                        else
                            basicTest.put("result", (result == 1));
                        continue;
                    }
                    basicTest.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
            }
            cursor.close();
            return basicTest;

        }catch(Exception e){
            Log.e(Constants.LOG_TAG,"Exception when retrieving basic test from DB",e);
            if(cursor != null)
                cursor.close();
        }
        return null;
    }

    public void resetAllBasicTests() {
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabase();
        }
        ContentValues values = new ContentValues();
        //write to basictests table
        values.putNull("exception");
        values.put("result",-1);

        try {
            db.update("basictests",values,null,null);
        }catch(SQLiteDatabaseLockedException e){
            Log.d(Constants.LOG_TAG,"resetAllBasicTests: DB is locked...retrying.");
            for(int i= 0;i < DB_LOCKED_RETRIES; i++){
                try{
                    db.update("basictests",values,null,null);
                    break;
                }
                catch(SQLiteDatabaseLockedException e2){
                    Log.d(Constants.LOG_TAG,"resetAllBasicTests: DB is still locked... trying again.");
                }
            }
        }
    }

    public int getNumberOfTotalNotPerformedTests() {
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabaseReadOnly();
        }
        Cursor cursor = db.query(
                "basictests",
                new String[]{"uuid"},
                "result = ? and exception IS NULL",
                new String[]{"-1"},
                null,
                null,
                null
        );

        if(cursor == null)
            return -1;
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public void markBasicTestChunkSuccessful(String basicTestChunkURL) {
        if(basicTestChunkURL == null)
            return;
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabase();
        }
        //Log.d(Constants.LOG_TAG,"Marking basicTestChunkURL: "+basicTestChunkURL+" as downloaded and parsed successfully in DB");
        ContentValues values = new ContentValues();
        //write to basictests table
        values.put("url",basicTestChunkURL);
        values.put("successful",1);
        db.insertOrThrow("basictest_chunks", null, values);
    }

    public boolean wasBasicTestChunkSuccessful(String basicTestChunkURL) {
        //make sure DB access is ready
        if(db == null || !db.isOpen()){
            db = PADatabaseManager.getInstance().openDatabase();
        }
        Cursor cursor = db.query(
                "basictest_chunks",
                new String[]{"successful"},
                "url = ? and successful = ?",
                new String[]{basicTestChunkURL,""+1},
                null,
                null,
                null
        );

        if(cursor == null)
            return false;
        if(cursor.getCount() == 1) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }
}
