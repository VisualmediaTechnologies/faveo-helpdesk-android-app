package co.helpdesk.faveo.frontend.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by narendra on 13/06/16.
 */
public class InternetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        int i = NetworkUtil.getConnectivityStatus(context);
        if (i == 0) {
            Toast.makeText(context, "No Internet Connection!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Connected to Internet!", Toast.LENGTH_LONG).show();
        }
    }
}
