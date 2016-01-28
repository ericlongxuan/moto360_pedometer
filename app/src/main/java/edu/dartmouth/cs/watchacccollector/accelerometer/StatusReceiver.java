package edu.dartmouth.cs.watchacccollector.accelerometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.dartmouth.cs.watchacccollector.MainActivity;

/**
 * Created by _ReacTor on 16/1/27.
 */
public class StatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals("edu.dartmouth.pedometer.CUSTOM_INTENT")){
            String type = intent.getExtras().getString("extra");
            Log.d("1111111", type);
        }
    }
}
