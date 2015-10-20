package eu.siacs.conversations.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import eu.siacs.conversations.Config;

/**
 * Created by hlew on 10/16/15.
 *
 * This class starts the EditAccountActivity to reconfigure the account for Remotium.
 * This approach is similar to the approach used in the
 * {@link Activity} class' startActivityForResult() implementation.
 */
public class Configurator extends Instrumentation {
    private static final String ACTION_KEY = "action_key";
    public static final String JSON_RESULTS = "json_results";
    public static final String RETURN_CODE = "return_code";
    public static final int RETURN_SUCCESS = 0;
    public static final int RETURN_FAILURE = -1;

    private Bundle mArguments;
    private int mReturnCode;
    private ActivityMonitor mActivityMonitor;
    private Object mSync = new Object();

    private Intent bundleToIntent(Bundle arguments) {
        Intent intent = new Intent(arguments.getString(ACTION_KEY));
        Log.d(Config.LOGTAG, intent.getAction().toString());
        intent.putExtras(arguments);
        return intent;
    }
    @Override
    public void onCreate(Bundle arguments) {
        Log.d(Config.LOGTAG, "Starting instrumentation to configure SecureIM: " + arguments.toString());
        mArguments = arguments;
        super.onCreate(arguments);
        start();
    }

    @Override
        public void onStart() {
        Bundle result;
        Log.d(Config.LOGTAG, "Instrumentation: onStart");
        mActivityMonitor = addMonitor("eu.siacs.conversations.ui.EditAccountActivity", null , false);
        myExecStartActivity();
        Log.d(Config.LOGTAG, "Instrumentation: before waitforMonitor  ");
        Activity activity = waitForMonitor(mActivityMonitor);
        Log.d(Config.LOGTAG, "Instrumentation: before waitforIdleSync  ");
        waitForIdleSync();
        result = jsonStringToBundle();

        finish(mReturnCode, result);
    }

    private void myExecStartActivity() {
        Log.d(Config.LOGTAG, "Instrumentation: Start activity");
        Intent i = bundleToIntent(mArguments);
        i.setComponent(new ComponentName("eu.siacs.conversations", "eu.siacs.conversations.ui.EditAccountActivity"));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        synchronized(mSync) {
            Log.d(Config.LOGTAG, "Instrumentation: StartActivitySync");
            startActivitySync(i);
        }
    }

    private Bundle jsonStringToBundle() {
        Context tCtx = getTargetContext();
        SharedPreferences prefs = tCtx.getSharedPreferences(tCtx.getPackageName(), Context.MODE_PRIVATE);
        String jsonResult = prefs.getString(JSON_RESULTS, "{}");
        mReturnCode = prefs.getInt(RETURN_CODE, RETURN_SUCCESS);

        Log.d(Config.LOGTAG, jsonResult);
        Bundle results = new Bundle();
        results.putString(JSON_RESULTS, jsonResult);
        results.putInt(RETURN_CODE, mReturnCode);

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(JSON_RESULTS).remove(RETURN_CODE).remove(Config.JID_COUNT).commit();

        return results;
    }

}
