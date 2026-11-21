package com.conc.focus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FocusTimerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        FocusState.isActive(context);
        FocusWidget.updateAll(context);
    }
}
