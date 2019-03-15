package com.polestar.task.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.polestar.task.ADErrorCode;
import com.polestar.task.IUserStatusListener;
import com.polestar.task.IVpnStatusListener;
import com.polestar.task.database.DatabaseApi;
import com.polestar.task.database.DatabaseImplFactory;
import com.polestar.task.network.datamodels.User;
import com.polestar.task.network.datamodels.VpnRequirement;
import com.polestar.task.network.responses.ServersResponse;

import java.util.HashSet;

import winterfell.flash.vpn.FlashApp;
import winterfell.flash.vpn.network.VPNServerIntermediaManager;
import winterfell.flash.vpn.utils.CommonUtils;
import winterfell.flash.vpn.utils.EventReporter;
import winterfell.flash.vpn.utils.MLogs;
import winterfell.flash.vpn.utils.PreferenceUtils;
import winterfell.flash.vpn.utils.RemoteConfig;

/**
 * Created by guojia on 2019/1/26.
 */

public class RewardInfoFetcher extends BroadcastReceiver{
    private final static String TAG = "RewardInfoFetcher";

    private Context mContext;
    private static RewardInfoFetcher sInstance;
    private Handler workHandler;
    private static long UPDATE_INTERVAL = 3600*1000;

    private final static long FORCE_UPDATE_INTERVAL = 2000;
    private final static int FORCE_RETRY_TIMES = 5;

    private final static int MSG_FETCH_INFO = 1;

    private DatabaseApi databaseApi;
    private int forceRetry;

    private HashSet<IRewardInfoFetchListener> mRegistry;
    private VPNServerIntermediaManager mVpnServerInterManager;

    public interface IRewardInfoFetchListener{
        void onFetched();
    }

    private RewardInfoFetcher(Context context) {
        mVpnServerInterManager = VPNServerIntermediaManager.getInstance(FlashApp.getApp());
        UPDATE_INTERVAL = RemoteConfig.getLong("config_update_interval_sec")*1000;
        mContext = context;
        forceRetry = 0;
        databaseApi = DatabaseImplFactory.getDatabaseApi(context, DatabaseImplFactory.TARGET_FLASH_VPN);
        mRegistry = new HashSet<>();
        HandlerThread thread = new HandlerThread("sync_task");
        thread.start();
        workHandler = new Handler(thread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MSG_FETCH_INFO:
                        long interval;
                        if (databaseApi.isDataAvailable()) {
                            forceRetry = 0;
                            interval = UPDATE_INTERVAL;
                        } else {
                            interval = forceRetry++ >= FORCE_RETRY_TIMES ? UPDATE_INTERVAL : FORCE_UPDATE_INTERVAL;
                        }
                        checkAndFetchInfo(!databaseApi.isDataAvailable());
                        workHandler.sendMessageDelayed(workHandler.obtainMessage(MSG_FETCH_INFO), interval);
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(this, filter);
    }

    //TODO need device id?
    private void checkAndFetchInfo(final boolean force) {
        MLogs.d(TAG, "checkAndFetchInfo force: " + force + " myId " + AppUser.getInstance().getMyId());
        if(!force && (System.currentTimeMillis() - PreferenceUtils.getLastUpdateTime()
                < UPDATE_INTERVAL)) {
            MLogs.d(TAG, "already fetched at " + PreferenceUtils.getLastUpdateTime());
            return;
        }
        VpnApiHelper.register(mContext, AppUser.getInstance().getMyId(), new IUserStatusListener() {
            @Override
            public void onRegisterSuccess(User user) {
                databaseApi.setUserInfo(user);
                MLogs.d(TAG, "register success " + user);
                VpnApiHelper.getVpnServers(AppUser.getInstance().getMyId(), new IVpnStatusListener(){


                    @Override
                    public void onAcquireSucceed(VpnRequirement requirement) {

                    }

                    @Override
                    public void onAcquireFailed(String publicIp, ADErrorCode code) {

                    }

                    @Override
                    public void onReleaseSucceed(String publicIp) {

                    }

                    @Override
                    public void onReleaseFailed(String publicIp, ADErrorCode code) {

                    }

                    @Override
                    public void onGetAllServers(ServersResponse servers) {
                        MLogs.i("onGetAllServers");
                        mVpnServerInterManager.updateRawServerInfo(servers);
                    }

                    @Override
                    public void onGeneralError(ADErrorCode code) {
                        EventReporter.rewardEvent("fetch_error_" + code.getErrCode());
                        MLogs.d(TAG, "onError " + code);
                    }
                },force);
            }

            @Override
            public void onRegisterFailed(ADErrorCode errorCode) {

            }

            @Override
            public void onGeneralError(ADErrorCode code) {
                EventReporter.rewardEvent("fetch_error_" + code.getErrCode());
                MLogs.d(TAG, "onError " + code);
            }
        }, force);

    }

    public static synchronized RewardInfoFetcher get(Context context) {
        if(sInstance == null) {
            sInstance = new RewardInfoFetcher(context);
        }
        return sInstance;
    }

    public void preloadRewardInfo() {
        MLogs.d(TAG, "preloadRewardInfo");
        workHandler.removeMessages(MSG_FETCH_INFO);
        workHandler.sendMessage(
                workHandler.obtainMessage(MSG_FETCH_INFO));
    }

    //Just start another round retry
    public void forceRefresh() {
        forceRetry = 0;
        preloadRewardInfo();
    }

    public synchronized void registerUpdateObserver(IRewardInfoFetchListener listener) {
        mRegistry.add(listener);
    }

    public synchronized void unregisterUpdateObserver(IRewardInfoFetchListener listener) {
        mRegistry.remove(listener);
    }
    //public void fetchRewardInfo(boolean force, )

    @Override
    public void onReceive(Context context, Intent intent) {
        MLogs.d("RewardInfoFetcher " + intent);
        if (CommonUtils.isNetworkAvailable(context)){
            preloadRewardInfo();
        }
    }
}