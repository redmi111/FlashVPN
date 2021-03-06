package com.polestar.clone.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.polestar.clone.client.core.VirtualCore;
import com.polestar.clone.client.env.Constants;
import com.polestar.clone.client.env.SpecialComponentList;
import com.polestar.clone.helper.collection.ArrayMap;
import com.polestar.clone.helper.utils.ComponentUtils;
import com.polestar.clone.helper.utils.Reflect;
import com.polestar.clone.helper.utils.VLog;
import com.polestar.clone.os.VUserHandle;
import com.polestar.clone.remote.BroadcastIntentData;
import com.polestar.clone.remote.PendingResultData;
import com.polestar.clone.server.pm.PackageSetting;
import com.polestar.clone.server.pm.VAppManagerService;
import com.polestar.clone.server.pm.parser.VPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mirror.android.app.ContextImpl;
import mirror.android.app.LoadedApkHuaWei;
import mirror.android.rms.resource.ReceiverResourceLP;
import mirror.android.rms.resource.ReceiverResourceM;
import mirror.android.rms.resource.ReceiverResourceN;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;

/**
 * @author Lody
 */

public class BroadcastSystem {

    private static final String TAG = BroadcastSystem.class.getSimpleName();
    /**
     * MUST < 10000.
     */
    private static final int BROADCAST_TIME_OUT = 7000;
    private static BroadcastSystem gDefault;

    private final ArrayMap<String, List<BroadcastReceiver>> mReceivers = new ArrayMap<>();
    private final Map<IBinder, BroadcastRecord> mBroadcastRecords = new HashMap<>();
    private final Context mContext;
    private final StaticScheduler mScheduler;
    private final TimeoutHandler mTimeoutHandler;
    private final VActivityManagerService mAMS;
    private final VAppManagerService mApp;

    private BroadcastSystem(Context context, VActivityManagerService ams, VAppManagerService app) {
        this.mContext = context;
        this.mApp = app;
        this.mAMS = ams;
        HandlerThread broadcastThread = new HandlerThread("BroadcastThread");
        HandlerThread anrThread = new HandlerThread("BroadcastAnrThread");
        broadcastThread.start();
        anrThread.start();
        mScheduler = new StaticScheduler(broadcastThread.getLooper());
        mTimeoutHandler = new TimeoutHandler(anrThread.getLooper());
        fuckHuaWeiVerifier();
    }

    public static void attach(VActivityManagerService ams, VAppManagerService app) {
        if (gDefault != null) {
            VLog.logbug(TAG, VLog.getStackTraceString(new IllegalStateException("gDefault reinit")));
        }
        gDefault = new BroadcastSystem(VirtualCore.get().getContext(), ams, app);
    }

    public static BroadcastSystem get() {
        return gDefault;
    }

    /**
     * FIX ISSUE #171:
     * java.lang.AssertionError: Register too many Broadcast Receivers
     * at android.app.LoadedApk.checkRecevierRegisteredLeakLocked(LoadedApk.java:772)
     * at android.app.LoadedApk.getReceiverDispatcher(LoadedApk.java:800)
     * at android.app.ContextImpl.registerReceiverInternal(ContextImpl.java:1329)
     * at android.app.ContextImpl.registerReceiver(ContextImpl.java:1309)
     * at com.lody.virtual.server.am.BroadcastSystem.startApp(BroadcastSystem.java:54)
     * at com.lody.virtual.server.pm.VAppManagerService.install(VAppManagerService.java:193)
     * at com.lody.virtual.server.pm.VAppManagerService.preloadAllApps(VAppManagerService.java:98)
     * at com.lody.virtual.server.pm.VAppManagerService.systemReady(VAppManagerService.java:70)
     * at com.lody.virtual.server.BinderProvider.onCreate(BinderProvider.java:42)
     */
    private void fuckHuaWeiVerifier() {

        if (LoadedApkHuaWei.mReceiverResource != null) {
            Object packageInfo = ContextImpl.mPackageInfo.get(mContext);
            if (packageInfo != null) {
                Object receiverResource = LoadedApkHuaWei.mReceiverResource.get(packageInfo);
                if (receiverResource != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Map map = Reflect.on(receiverResource).get("mWhiteListMap");
                        List list = (List) map.get(0);
                        if(list == null) {
                            list = new ArrayList();
                            map.put(Integer.valueOf(0), list);
                        }

                        list.add(this.mContext.getPackageName());
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (ReceiverResourceN.mWhiteList != null) {
                            List<String> whiteList = ReceiverResourceN.mWhiteList.get(receiverResource);
                            List<String> newWhiteList = new ArrayList<>();
                            // Add our package name to the white list.
                            newWhiteList.add(mContext.getPackageName());
                            if (whiteList != null) {
                                newWhiteList.addAll(whiteList);
                            }
                            ReceiverResourceN.mWhiteList.set(receiverResource, newWhiteList);
                        }

                    } else {
                        if (ReceiverResourceM.mWhiteList != null) {
                            String[] whiteList = ReceiverResourceM.mWhiteList.get(receiverResource);
                            List<String> newWhiteList = new LinkedList<>();
                            Collections.addAll(newWhiteList, whiteList);
                            // Add our package name to the white list.
                            newWhiteList.add(mContext.getPackageName());
                            ReceiverResourceM.mWhiteList.set(receiverResource, newWhiteList.toArray(new String[newWhiteList.size()]));
                        } else if (ReceiverResourceLP.mResourceConfig != null) {
                            // Just clear the ResourceConfig.
                            ReceiverResourceLP.mResourceConfig.set(receiverResource, null);
                        }
                    }
                }
            }
        }
    }

    public void startApp(VPackage p) {
        PackageSetting setting = (PackageSetting) p.mExtras;
        VLog.d("BroadcastSystem", "startApp " + p.packageName);
        for (VPackage.ActivityComponent receiver : p.receivers) {
            ActivityInfo info = receiver.info;
            List<BroadcastReceiver> receivers = mReceivers.get(p.packageName);
            if (receivers == null) {
                receivers = new ArrayList<>();
                mReceivers.put(p.packageName, receivers);
            }
            String componentAction = String.format(Constants.VA_INTENT_KEY_COMPONENT_ACTION_FMT, info.packageName, info.name);
            IntentFilter componentFilter = new IntentFilter(componentAction);
            BroadcastReceiver r = new StaticBroadcastReceiver(setting.appId, info, componentFilter);
            mContext.registerReceiver(r, componentFilter, null, mScheduler);
            VLog.d("BroadcastSystem", "register " + componentFilter.getAction(0));
            receivers.add(r);
            for (VPackage.ActivityIntentInfo ci : receiver.intents) {
                IntentFilter cloneFilter = new IntentFilter(ci.filter);
                SpecialComponentList.protectIntentFilter(cloneFilter, ci.activity.getComponentName().getPackageName());
                r = new StaticBroadcastReceiver(setting.appId, info, cloneFilter);
                mContext.registerReceiver(r, cloneFilter, null, mScheduler);
                receivers.add(r);
            }
        }
    }


    public void stopApp(String packageName) {
        synchronized (mBroadcastRecords) {
            Iterator<Map.Entry<IBinder, BroadcastRecord>> iterator = mBroadcastRecords.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<IBinder, BroadcastRecord> entry = iterator.next();
                BroadcastRecord record = entry.getValue();
                if (record.receiverInfo.packageName.equals(packageName)) {
                    record.pendingResult.finish();
                    iterator.remove();
                }
            }
        }
        synchronized (mReceivers) {
            List<BroadcastReceiver> receivers = mReceivers.get(packageName);
            if (receivers != null) {
                for (BroadcastReceiver r : receivers) {
                    mContext.unregisterReceiver(r);
                }
            }
            mReceivers.remove(packageName);
        }
    }

    void broadcastFinish(PendingResultData res) {
        synchronized (mBroadcastRecords) {
            BroadcastRecord record = mBroadcastRecords.remove(res.mToken);
            if (record == null) {
                VLog.e(TAG, "Unable to find the BroadcastRecord by token: " + res.mToken);
            }
        }
        mTimeoutHandler.removeMessages(0, res.mToken);
        res.finish();
    }

    void broadcastSent(int vuid, ActivityInfo receiverInfo, PendingResultData res) {
        BroadcastRecord record = new BroadcastRecord(vuid, receiverInfo, res);
        synchronized (mBroadcastRecords) {
            mBroadcastRecords.put(res.mToken, record);
        }
        Message msg = new Message();
        msg.obj = res.mToken;
        mTimeoutHandler.sendMessageDelayed(msg, BROADCAST_TIME_OUT);
    }

    private static final class StaticScheduler extends Handler {

        StaticScheduler(Looper looper) {
            super(looper);
        }
    }

    private static final class BroadcastRecord {
        int vuid;
        ActivityInfo receiverInfo;
        PendingResultData pendingResult;

        BroadcastRecord(int vuid, ActivityInfo receiverInfo, PendingResultData pendingResult) {
            this.vuid = vuid;
            this.receiverInfo = receiverInfo;
            this.pendingResult = pendingResult;
        }
    }

    private final class TimeoutHandler extends Handler {

        TimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            IBinder token = (IBinder) msg.obj;
            BroadcastRecord r = mBroadcastRecords.remove(token);
            if (r != null) {
                VLog.w(TAG, "Broadcast timeout, cancel to dispatch it. info: " + r.receiverInfo.toString());
                r.pendingResult.finish();
            }
        }
    }


    private final class StaticBroadcastReceiver extends BroadcastReceiver {
        private int appId;
        private ActivityInfo info;
        @SuppressWarnings("unused")
        private IntentFilter filter;

        private StaticBroadcastReceiver(int appId, ActivityInfo info, IntentFilter filter) {
            this.appId = appId;
            this.info = info;
            this.filter = filter;
        }

        @Override
		public void onReceive(Context context, final Intent intent) {
            VLog.logbug("StaticBroadcastReceiver", "E onReceive " + intent.toString());
            if (mApp.isBooting()) {
                return;
            }
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY) != 0 || isInitialStickyBroadcast()) {
                return;
            }
            try {
                String privilegePkg = intent.getStringExtra("_VA_|_privilege_pkg_");
                if (privilegePkg != null && !info.packageName.equals(privilegePkg)) {
                    return;
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }

            BroadcastIntentData broadcastIntentData = null;

            if (intent.getExtras() != null) {
                intent.setExtrasClassLoader(BroadcastIntentData.class.getClassLoader());
                broadcastIntentData = intent.getParcelableExtra(Constants.VA_INTENT_KEY_BRDATA);
            }
            if (broadcastIntentData == null) {
                VLog.logbug(TAG,"intent from system " + intent);
                Intent realIntent = intent;
                try {
                    if (intent.getExtras() != null && intent.hasExtra(Constants.VA_INTENT_KEY_INTENT)) {
                        realIntent = intent.getParcelableExtra(Constants.VA_INTENT_KEY_INTENT);
                        VLog.logbug(TAG, "Bug intent  " + intent);
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
                intent.setPackage(null);
                broadcastIntentData = new BroadcastIntentData(-1, realIntent, null, null);
            }
            if (broadcastIntentData.pkg != null && !broadcastIntentData.pkg.equals(info.packageName)) {
                return;
            }
            if (broadcastIntentData.componentName != null && !ComponentUtils.toComponentName(info).equals(broadcastIntentData.componentName)) {
                // Verify the component.
                return;
            }
            final PendingResult result = goAsync();
            final BroadcastIntentData data = new BroadcastIntentData(broadcastIntentData);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mAMS.handleStaticBroadcast(appId, info, data, new PendingResultData(result))) {
                        result.finish();
        //                if (mOrderedHint) {
        //                    am.finishReceiver(mToken, mResultCode, mResultData, mResultExtras,
        //                            mAbortBroadcast, mFlags);
        //                } else {
        //                    // This broadcast was sent to a component; it is not ordered,
        //                    // but we still need to tell the activity manager we are done.
        //                    am.finishReceiver(mToken, 0, null, null, false, mFlags);
        //                }
                    }
                }
            }).start();

            VLog.d("StaticBroadcastReceiver", "X onReceive " + intent.toString());
        }
    }
}
