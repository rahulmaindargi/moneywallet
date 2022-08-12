package com.rahul.moneywallet.service.syncworkers.devicesync;

import static com.rahul.moneywallet.service.syncworkers.devicesync.SyncDeviceHelper.SERVICE_NAME;
import static com.rahul.moneywallet.service.syncworkers.devicesync.SyncDeviceHelper.SERVICE_TYPE;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class NSDClientHelper {
    public static final String CLIENT_WORKER_NAME = "WriterWorker";
    private final Context context;
    private final NsdManager nsdManager;

    private NsdManager.DiscoveryListener discoveryListener;

    public NSDClientHelper(@NonNull Context context) {

        this.context = context;
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    private NsdManager.DiscoveryListener initializeDiscoveryListener() {
        NsdManager.ResolveListener resolveListener = initializeResolveListener();
        AtomicBoolean resolveListenerInUse = new AtomicBoolean(false);
        return new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("SyncDeviceActivity", "Start Discovery Failed with Error code <" + errorCode + ">");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("SyncDeviceActivity", "Stop Discovery Failed with Error code <" + errorCode + ">");
            }

            @Override
            public void onDiscoveryStarted(String s) {
                Log.i("SyncDeviceActivity", "On Discovery Started");
            }

            @Override
            public void onDiscoveryStopped(String s) {
                Log.i("SyncDeviceActivity", "On Stopped Discovery");
//                try {
//                    List<WorkInfo> writerWorker = WorkManager.getInstance(context).getWorkInfosForUniqueWork("WriterWorker").get();
//                    if (writerWorker != null) {
//                        writerWorker.stream()
//                                .filter(wi -> !wi.getState().isFinished())
//                                .findAny()
//                                .ifPresent(wi -> WorkManager.getInstance(context).cancelUniqueWork("WriterWorker"));
//                    }
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                    Log.e("SyncDeviceActivity", "Failed to Cancel Writer", e);
//                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                Log.i("SyncDeviceActivity", "Service Discovery Success " + nsdServiceInfo);
                if (!nsdServiceInfo.getServiceType().equals(SERVICE_TYPE) && !nsdServiceInfo.getServiceType().equals(SERVICE_TYPE + ".")) { // this . required for some DNS naming not mentioned in docs.
                    Log.d("SyncDeviceActivity", "Discovery Unknown service type " + nsdServiceInfo.getServiceType() + " <" + SERVICE_TYPE + ">");
                }
                // Not needed I think as we will either be Server or Client never both at same time.
//                else if (nsdServiceInfo.getServiceName().equals(serviceName)) {
//                    Log.d("SyncDeviceActivity", "Discovery Same Machine " + nsdServiceInfo.getServiceName());
//                }
                else if (nsdServiceInfo.getServiceName().contains(SERVICE_NAME)) {
                    if (!resolveListenerInUse.getAndSet(true)) {
                        nsdManager.resolveService(nsdServiceInfo, resolveListener);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                Log.i("SyncDeviceActivity", "Service Lost");
                resolveListenerInUse.set(false);
//                try {
//                    List<WorkInfo> writerWorker = WorkManager.getInstance(context).getWorkInfosForUniqueWork("WriterWorker").get();
//                    if (writerWorker != null) {
//                        writerWorker.stream()
//                                .filter(wi -> !wi.getState().isFinished())
//                                .findAny()
//                                .ifPresent(wi -> WorkManager.getInstance(context).cancelUniqueWork("WriterWorker"));
//                    }
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                    Log.e("SyncDeviceActivity", "Failed to Cancel Writer", e);
//                }
            }
        };
    }

    private NsdManager.ResolveListener initializeResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e("SyncDeviceActivity", "Resolve Failed Error code " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                Log.i("SyncDeviceActivity", "Resolve succeeded " + nsdServiceInfo);
//                if (nsdServiceInfo.getServiceName().equals(serviceName)) {
//                    // SAME Device
//                    return;
//                }

                InetAddress host = nsdServiceInfo.getHost();
                int port = nsdServiceInfo.getPort();

                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ClientWorker.class)
                        .setInputData(new Data.Builder()
                                .putString("host", host.getHostAddress())
                                .putInt("port", port)
                                .build())
                        .build();
                Operation writerWorker = WorkManager.getInstance(context).enqueueUniqueWork(CLIENT_WORKER_NAME, ExistingWorkPolicy.KEEP, request);
//                new Handler(Looper.getMainLooper()).post(() ->
//                        writerWorker.getState().observe(new SyncDeviceHelper.TempLifeCycleOwner(), state -> {
//                            if (state instanceof Operation.State.SUCCESS) {
//                                Log.i("SyncDeviceActivity", "Writer Started");
//                                new Handler(Looper.getMainLooper()).post(() ->
//                                        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("WriterWorker")
//                                                .observe(new SyncDeviceHelper.TempLifeCycleOwner(),
//                                                        workInfos -> workInfos.stream()
//                                                                .filter(wi -> wi.getState().isFinished())
//                                                                .findAny()
//                                                                .ifPresent(wi -> downLatch.countDown()
//                                                                )
//                                                )
//                                );
//
//
//                            } else if (state instanceof Operation.State.FAILURE) {
//                                Log.e("SyncDeviceActivity", "Writer Failed to start", ((Operation.State.FAILURE) state).getThrowable());
//                                downLatch.countDown();
//                            }
//                        })
//                );
            }
        };
    }

    public void startDiscovery() {
        discoveryListener = initializeDiscoveryListener();
        Log.d("SyncDeviceActivity", "Trying to DISCOVER");
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        Log.d("SyncDeviceActivity", "AWAITING COMPLETION");


    }

    public void stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener);
        } catch (IllegalArgumentException e) {
            // DO Nothing
        }
    }

}
