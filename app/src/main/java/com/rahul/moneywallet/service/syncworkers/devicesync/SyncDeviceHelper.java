package com.rahul.moneywallet.service.syncworkers.devicesync;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class SyncDeviceHelper {
    public static final String END_OF_DATA = "END OF DATA";
    public static final String OPERATION_RECEIVE = "receive";
    public static final String OPERATION_WRITE = "write";
    public static final String OPERATION_KEY = "operation";

    static final String SERVICE_TYPE = "_moneywalletsync._tcp";
    static final String SERVICE_NAME = "MoneyWalletSyncService";

    static class TempLifeCycleOwner implements LifecycleOwner {

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return new LifecycleRegistry(this);
        }
    }

}
