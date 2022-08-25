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

import java.io.IOException;
import java.net.ServerSocket;

public class NSDServiceRegistrationHelper {
    public static final String SERVER_WORKER_NAME = "ServerWorker";
    private final Context context;
    private final NsdManager nsdManager;
    private int serverSocketPort;
    private String serviceName;
    private NsdManager.RegistrationListener registrationListener;

    public NSDServiceRegistrationHelper(@NonNull Context context) {
        this.context = context;
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    private NsdServiceInfo initServiceInfo() {
        NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setServiceName(SERVICE_NAME);
        nsdServiceInfo.setServiceType(SERVICE_TYPE);
        int localPort = getAvailablePort();
        serverSocketPort = localPort;
        nsdServiceInfo.setPort(localPort);
        return nsdServiceInfo;
    }

    private int getAvailablePort() {
        serverSocketPort = 0;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Log.i("SyncDeviceActivity", "Local port" + serverSocket.getLocalPort());
            serverSocketPort = serverSocket.getLocalPort();
            return serverSocketPort;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


    private NsdManager.RegistrationListener initializeRegistrationListener() {
        return new NsdManager.RegistrationListener() {

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e("SyncDeviceActivity", "Registration Failed with Error code <" + errorCode + ">");
                nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e("SyncDeviceActivity", "UnRegistration Failed with Error code <" + errorCode + ">");
                nsdManager.unregisterService(this);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                Log.i("SyncDeviceActivity", "Service Registered");
                serviceName = nsdServiceInfo.getServiceName();
                // NOTE nsdServiceInfo get port return 0 always
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ServerWorker.class)
                        .setInputData(new Data.Builder().putInt("port", serverSocketPort).build())
                        .build();

                Operation listenerWorker = WorkManager.getInstance(context).enqueueUniqueWork(SERVER_WORKER_NAME, ExistingWorkPolicy.KEEP, workRequest);
//                new Handler(Looper.getMainLooper()).post(() ->
//                        listenerWorker.getState().observe(new SyncDeviceHelper.TempLifeCycleOwner(), state -> {
//                            if (state instanceof Operation.State.SUCCESS) {
//                                Log.i("SyncDeviceActivity", "Listener Started");
//                                new Handler(Looper.getMainLooper()).post(() ->
//                                        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("ListenerWorker")
//                                                .observe(new SyncDeviceHelper.TempLifeCycleOwner(),
//                                                        workInfos -> workInfos.stream()
//                                                                .filter(wi -> wi.getState().isFinished())
//                                                                .findAny()
//                                                                .ifPresent(wi -> downLatch.countDown())
//                                                )
//                                );
//                            } else if (state instanceof Operation.State.FAILURE) {
//                                Log.e("SyncDeviceActivity", "Listener Failed to start", ((Operation.State.FAILURE) state).getThrowable());
//                                downLatch.countDown();
//                            }
//                        })
//                );
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.i("SyncDeviceActivity", "Service Unregistered " + this);
//                try {
//                    List<WorkInfo> listenerWorker = WorkManager.getInstance(context).getWorkInfosForUniqueWork("ListenerWorker").get();
//                    if (listenerWorker != null) {
//                        listenerWorker.stream()
//                                .filter(wi -> !wi.getState().isFinished())
//                                .findAny()
//                                .ifPresent(wi -> WorkManager.getInstance(context).cancelUniqueWork("ListenerWorker"));
//                    }
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                    Log.e("SyncDeviceActivity", "Failed to Cancel ", e);
//                }
            }
        };
    }

    public void registerService() {
        registrationListener = initializeRegistrationListener();
        NsdServiceInfo nsdServiceInfo = initServiceInfo();
        Log.d("SyncDeviceActivity", "Trying to Register " + registrationListener);
        nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }


    public void deRegisterService() {
        try {
            nsdManager.unregisterService(registrationListener);
        } catch (IllegalArgumentException iae) {
            // Do Nothing
        }
    }
}
