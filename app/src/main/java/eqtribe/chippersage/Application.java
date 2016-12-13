package eqtribe.chippersage;

import android.app.Activity;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.util.HashMap;
import java.util.Map;

import eqtribe.chippersage.util.StringUtil;

import static com.couchbase.lite.auth.AESSecureTokenStore.TAG;

/**
 * Created by Shushmit Yadav on 07-12-2016.
 */

public class Application extends android.app.Application implements Replication.ChangeListener {

    // Storage Type: .SQLITE_STORAGE or .FORESTDB_STORAGE
    private static final String STORAGE_TYPE = Manager.SQLITE_STORAGE;
    // Encryption (Don't store encryption key in the source code. We are doing it here just as an example):
    private static final boolean ENCRYPTION_ENABLED = false;
    private static final String ENCRYPTION_KEY = "shushmit";
    // Logging:
    private static final boolean LOGGING_ENABLED = true;

    private static final String GUEST_DATABASE_NAME = "eqtribe";
    private Manager mManager;
    private Database mDatabase;
    private Replication mPull;
    private Replication mPush;
    private Throwable mReplError;
    private String mCurrentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        enableLogging();
    }
    private void enableLogging() {
        if (LOGGING_ENABLED) {
            Manager.enableLogging(TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);
        }
    }
    private Manager getManager() {
        if (mManager == null) {
            try {
                AndroidContext context = new AndroidContext(getApplicationContext());
                mManager = new Manager(context, Manager.DEFAULT_OPTIONS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot create Manager object", e);
            }
        }
        return mManager;
    }

    public Database getDatabase() {
        return mDatabase;
    }

    private void setDatabase(Database database) {
        this.mDatabase = database;
    }

    private Database getUserDatabase(String name) {
        try {
            String dbName = "db" + StringUtil.MD5(name);
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            options.setStorageType(STORAGE_TYPE);
            options.setEncryptionKey(ENCRYPTION_ENABLED ? ENCRYPTION_KEY : null);
            return getManager().openDatabase(dbName, options);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot create database for name: " + name, e);
        }
        return null;
    }

    @Override
    public void changed(Replication.ChangeEvent event) {

    }

    public void registerNewUser(Activity activity, String userName, String password, String name){
        setCurrentUserId(userName);
        setDatabase(getUserDatabase(userName));
        String profileDocID = "p:" + userName;
        Document profile = mDatabase.getExistingDocument(profileDocID);
        if (profile == null) {
            try {
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put("type", "profile");
                properties.put("user_name", userName);
                properties.put("name", name);

                profile = mDatabase.getDocument(profileDocID);
                profile.putProperties(properties);

                // Migrate guest data to user:
                //UserProfile.migrateGuestData(getUserDatabase(GUEST_DATABASE_NAME), profile);
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Cannot create a new user profile", e);
            }
        }
    }

    private void setCurrentUserId(String userId) {
        this.mCurrentUserId = userId;
    }

    public String getCurrentUserId() {
        return this.mCurrentUserId;
    }
}
