package org.trace.tracking.store;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.trace.tracking.Constants;


/**
 * Created by Rodrigo Lourenço on 15/02/2016.
 */
public class TRACEStoreReceiver extends BroadcastReceiver {

    public TRACEStoreReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {


        if(intent.hasExtra(Constants.FIRST_TIME_BROADCAST)
                && intent.getBooleanExtra(Constants.FIRST_TIME_BROADCAST, true)){

            /**
             * TODO: send and action that means that a login activity should be presented.
            context.startActivity(
                    new Intent(context, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
             **/

            return;
        }

    }
}
