package org.trace.trackerproto.ui;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

import org.trace.tracking.store.TRACEStore;

public class DynamicInitActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;
        //Force Login if it's the first time
        if(TRACEStore.Client.isFirstTime(this))
            intent = new Intent(this, LoginActivity.class);
        else
            intent = new Intent(this, MainActivity.class);

        startActivity(intent);
        finish();
    }

}
