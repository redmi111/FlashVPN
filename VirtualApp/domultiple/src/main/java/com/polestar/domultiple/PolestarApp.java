package com.polestar.domultiple;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.polestar.ad.AdUtils;
import com.polestar.ad.IAdEventLogger;
import com.polestar.ad.SDKConfiguration;
import com.polestar.booster.BoosterSdk;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.polestar.clone.BitmapUtils;
import com.polestar.clone.CustomizeAppData;
import com.polestar.clone.client.VClientImpl;
import com.polestar.clone.client.core.CrashHandler;
import com.polestar.clone.client.core.VirtualCore;
import com.polestar.clone.client.env.SpecialComponentList;
import com.polestar.clone.client.stub.VASettings;
import com.polestar.clone.helper.utils.VLog;
import com.polestar.ad.AdConfig;
import com.polestar.ad.AdConstants;
import com.polestar.ad.adapters.FuseAdLoader;
import com.polestar.domultiple.billing.BillingProvider;
import com.polestar.domultiple.clone.CloneApiDelegate;
import com.polestar.domultiple.clone.CloneComponentDelegate;
import com.polestar.domultiple.components.AppMonitorService;
import com.polestar.domultiple.components.receiver.PackageChangeReceiver;
import com.polestar.domultiple.components.ui.AppLoadingActivity;
import com.polestar.domultiple.notification.QuickSwitchNotification;
import com.polestar.domultiple.task.AppUser;
import com.polestar.domultiple.utils.CommonUtils;
import com.polestar.domultiple.utils.DoConfig;
import com.polestar.domultiple.utils.EventReporter;
import com.polestar.domultiple.utils.MLogs;
import com.polestar.domultiple.utils.PreferencesUtils;
import com.polestar.domultiple.utils.RemoteConfig;
import com.polestar.task.database.DatabaseImplFactory;
import com.polestar.task.network.Configuration;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.util.List;


/**
 * Created by PolestarApp on 2017/7/15.
 */

public class PolestarApp extends MultiDexApplication {

    private static PolestarApp gDefault;

    public static PolestarApp getApp() {
        return gDefault;
    }

    static {
        SpecialComponentList.APP_LOADING_ACTIVITY = AppLoadingActivity.class.getName();
        BitmapUtils.APP_ICON_RADIUS = 4;
        BitmapUtils.APP_ICON_PADDING = 3;
    }

    public static boolean isArm64() {
        return getApp().getPackageName().endsWith("arm64");
    }

    public static boolean isSupportPkgExist() {
        if (isArm64()) {
            return  true;
        } else {
            try{
                ApplicationInfo ai = getApp().getPackageManager().getApplicationInfo(AppConstants.ARM64_SUPPORT_PKG,0);
                if (ai != null) {
                    return true;
                }
            }catch(Exception ex){

            }
            return false;
        }
    }

    public static boolean isPrimaryPkgExist() {
        if(isArm64()) {
            try{
                ApplicationInfo ai = getApp().getPackageManager().getApplicationInfo(getApp().getPackageName().replace(".arm64",""),0);
                if (ai != null) {
                    return true;
                }
            }catch(Exception ex){

            }
            return false;
        } else {
            return true;
        }
    }

    public static boolean needAd() {
        return !(isArm64());
    }

    public static boolean isOpenLog(){
        boolean ret = false;
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/polelog");
            ret = file.exists();
            if (ret) {
                Log.d(MLogs.DEFAULT_TAG, "log opened by file");
            }
        }catch (Exception ex){

        }
        return  ret;
    }

    private String currentAdPkg;
    private int currentAdUser = -1;
    public void setCurrentAdClone(String pkg, int userId) {
        currentAdPkg = pkg;
        currentAdUser = userId;
    }

    public CustomizeAppData getCurrentCustomizeData() {
        if (currentAdPkg != null && currentAdUser != -1) {
            return CustomizeAppData.loadFromPref(currentAdPkg, currentAdUser);
        }
        return null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        Log.d(MLogs.DEFAULT_TAG, "APP version: " + BuildConfig.VERSION_NAME + " Type: " + BuildConfig.BUILD_TYPE);
        Log.d(MLogs.DEFAULT_TAG, "LIB version: " + com.polestar.clone.BuildConfig.VERSION_NAME + " Type: " + com.polestar.clone.BuildConfig.BUILD_TYPE );

        super.attachBaseContext(base);
        gDefault = this;
        try {
            VASettings.ENABLE_IO_REDIRECT = true;
            VASettings.ENABLE_INNER_SHORTCUT = false;
            VASettings.ENABLE_GMS = !PreferencesUtils.isLiteMode();
            Log.d(MLogs.DEFAULT_TAG, "GMS state: " + VASettings.ENABLE_GMS);
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initAd() {
//        MobileAds.initialize(gDefault, "ca-app-pub-5490912237269284~2442960626");
        SDKConfiguration.Builder builder = new SDKConfiguration.Builder();
        if (!needAd()) {
            for (String s : FuseAdLoader.SUPPORTED_TYPES) {
                builder.disableAdType(s);
            }
        } else {
            builder.mopubAdUnit("41988cc0fe194791a6b5cf6bb82290ca")
                    .admobAppId("ca-app-pub-5490912237269284~2442960626")
                    .ironSourceAppKey("8671d87d");
        }
        FuseAdLoader.init(new FuseAdLoader.ConfigFetcher() {
            @Override
            public boolean isAdFree(String slot) {
                return PreferencesUtils.isAdFree();
            }

            @Override
            public List<AdConfig> getAdConfigList(String slot) {
                return RemoteConfig.getAdConfigList(slot);
            }
        }, getApp(), builder.build());
        AdUtils.setEventLogger(new IAdEventLogger() {
            @Override
            public void trackEvent(String slot, String event) {
                EventReporter.generalEvent("ad_"+ slot+ "_" + event);
            }
        });
    }
    @Override
    public void onCreate() {
        super.onCreate();
        VLog.setKeyLogger(new VLog.IKeyLogger() {
            @Override
            public void keyLog(Context context, String tag, String log) {
                MLogs.logBug(tag,log);
                EventReporter.keyLog(gDefault, tag, log);
            }

            @Override
            public void logBug(String tag, String log) {
                MLogs.logBug(tag, log);
            }
        });
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                MLogs.d("Main process create");
                FirebaseApp.initializeApp(gDefault);
                RemoteConfig.init();
                //ImageLoaderUtil.asyncInit(gDefault);
                registerActivityLifecycleCallbacks(new AcivityLifeCycleListener());
                EventReporter.init(gDefault);
                DoConfig.get();
                BillingProvider.get();
//                if (needAd()) {
                    initAd();

                if (AppUser.check() && AppUser.isRewardEnabled()) {
                    Configuration.URL_PREFIX = RemoteConfig.getString("config_task_server");
                    Configuration.APP_VERSION_CODE = BuildConfig.VERSION_CODE;
                    Configuration.PKG_NAME = BuildConfig.APPLICATION_ID;
                    DatabaseImplFactory.CONF_NEED_TASK = true;
                    DatabaseImplFactory.CONF_NEED_PRODUCT = false;
                    FuseAdLoader.setUserId(AppUser.getInstance().getMyId());
                }
                    //CloneManager.getInstance(gDefault).loadClonedApps(gDefault, null);
                    //
                    BoosterSdk.BoosterConfig boosterConfig = new BoosterSdk.BoosterConfig();
                    if (BuildConfig.DEBUG) {
                        boosterConfig.autoAdFirstInterval = 0;
                        boosterConfig.autoAdInterval = 0;
                        boosterConfig.isUnlockAd = true;
                        boosterConfig.isInstallAd = true;
                        boosterConfig.avoidShowIfHistory = false;

                    } else {
                        boosterConfig.autoAdFirstInterval = RemoteConfig.getLong("auto_ad_first_interval") * 1000;
                        boosterConfig.autoAdInterval = RemoteConfig.getLong("auto_ad_interval") * 1000;
                        boosterConfig.isUnlockAd = RemoteConfig.getBoolean("allow_unlock_ad");
                        boosterConfig.isInstallAd = RemoteConfig.getBoolean("allow_install_ad");
                        boosterConfig.avoidShowIfHistory = RemoteConfig.getBoolean("avoid_ad_if_history");
                        if (!needAd()) {
                            boosterConfig.isUnlockAd = false;
                            boosterConfig.isInstallAd = false;
                        }

                    }
                    BoosterSdk.BoosterRes res = new BoosterSdk.BoosterRes();
                    res.outterWheelImage = R.drawable.booster_ic_wheel_outside;
                    res.innerWheelImage = R.drawable.booster_ic_wheel_inside;
                    res.titleString = R.string.boost_title;
                    res.boosterShorcutIcon = R.drawable.booster_shortcut;
                    BoosterSdk.init(gDefault, boosterConfig, res, new BoosterSdk.IEventReporter() {
                        @Override
                        public void reportWake(String s) {
                            EventReporter.reportWake(gDefault, s);
                        }

                        @Override
                        public void reportEvent(String s, Bundle b) {
                            FirebaseAnalytics.getInstance(PolestarApp.getApp()).logEvent(s, b);
                        }
                    });

                    //BoosterSdk.setMemoryThreshold(20);
                    //BoosterSdk.showSettings(this);
                AppLoadingActivity.preloadAd(getApp());
                AppMonitorService.preloadAd(null, 0);
                    initReceiver();
                if (QuickSwitchNotification.isEnable()) {
                    QuickSwitchNotification.getInstance(gDefault).init();
                }
//                }
            }

            @Override
            public void onVirtualProcess() {
                MLogs.d("Virtual process create");
                DoConfig.get();
                CloneComponentDelegate delegate = new CloneComponentDelegate();
                delegate.asyncInit();
                virtualCore.setComponentDelegate(delegate);

                virtualCore.setAppApiDelegate(new CloneApiDelegate());
            }

            @Override
            public void onServerProcess() {
                MLogs.d("Server process create");
                try {
                    VirtualCore.get().setAppRequestListener(new VirtualCore.AppRequestListener() {
                        @Override
                        public void onRequestInstall(String path) {
                            //We can start AppInstallActivity TODO
                        }

                        @Override
                        public void onRequestUninstall(String pkg) {

                        }
                    });
                }catch (Exception ex) {
                    MLogs.logBug(ex);
                }
                FirebaseApp.initializeApp(gDefault);
                RemoteConfig.init();
                DoConfig.get();
                CloneComponentDelegate delegate = new CloneComponentDelegate();
                delegate.asyncInit();
                VirtualCore.get().setComponentDelegate(delegate);
                initAd();
//                if (QuickSwitchNotification.getInstance(gDefault).isEnable()) {
//                    QuickSwitchNotification.getInstance(gDefault).init();
//                }
            }
        });

        try {
            // asyncInit exception handler and bugly before attatchBaseContext and appOnCreate
            final MAppCrashHandler ch = new MAppCrashHandler(this, Thread.getDefaultUncaughtExceptionHandler());
            Thread.setDefaultUncaughtExceptionHandler(ch);
            VirtualCore.get().setCrashHandler(new CrashHandler() {
                @Override
                public void handleUncaughtException(Thread t, Throwable e) {
                    ch.uncaughtException(t, e);
                }
            });
            initBugly(gDefault);
        }catch (Exception e){
            e.printStackTrace();
        }
        if (isOpenLog() || !AppConstants.IS_RELEASE_VERSION  || BuildConfig.DEBUG) {
            VLog.openLog();
            VLog.d(MLogs.DEFAULT_TAG, "VLOG is opened");
            MLogs.DEBUG = true;
            AdConstants.DEBUG = true;
            BoosterSdk.DEBUG = true;
        }

    }

    private class MAppCrashHandler implements Thread.UncaughtExceptionHandler {

        private Context context;
        private Thread.UncaughtExceptionHandler orig;
        MAppCrashHandler(Context c, Thread.UncaughtExceptionHandler orig) {
            context = c;
            this.orig = orig;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            MLogs.logBug("uncaughtException");

            String pkg;
            int tag;
            //CrashReport.startCrashReport();
            //1. innerContext = null, internal error in Pb
            if (VirtualCore.get() != null
                    && (VirtualCore.get().isMainProcess() )) {
                MLogs.logBug("Super Clone main app exception, exit.");
                pkg = "main";
                tag = AppConstants.CrashTag.MAPP_CRASH;
                //CrashReport.setUserSceneTag(context, AppConstants.CrashTag.MAPP_CRASH);
            } else if(VirtualCore.get()!= null && VirtualCore.get().isServerProcess()){
                MLogs.logBug("Server process crash!");
                pkg = "server";
                tag = AppConstants.CrashTag.SERVER_CRASH;
                //CrashReport.setUserSceneTag(context, AppConstants.CrashTag.SERVER_CRASH);
            } else {
                MLogs.logBug("Client process crash!");
                pkg = VClientImpl.get() == null? null: VClientImpl.get().getCurrentPackage();
                tag = AppConstants.CrashTag.CLONE_CRASH;
                //CrashReport.setUserSceneTag(context, AppConstants.CrashTag.CLONE_CRASH);
            }
            MLogs.logBug(MLogs.getStackTraceString(ex));

            ActivityManager.RunningAppProcessInfo info = CommonUtils.getForegroundProcess(context);
            boolean forground = false;
            if (info != null && android.os.Process.myPid() == info.pid) {
                MLogs.logBug("forground crash");
                forground = true;
                //CrashReport.setUserSceneTag(context, AppConstants.CrashTag.FG_CRASH);
            }
            Intent crash = new Intent("appclone.intent.action.SHOW_CRASH_DIALOG");
            crash.putExtra("package", pkg);
            crash.putExtra("forground", forground);
            crash.putExtra("exception", ex);
            crash.putExtra("tag", tag);
            sendBroadcast(crash);
            //CrashReport.postCatchedException(ex);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }
    private void setDefaultUncaughtExceptionHandler(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new MAppCrashHandler(context, Thread.getDefaultUncaughtExceptionHandler()));
    }

    private void initReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            getApp().registerReceiver(new PackageChangeReceiver(),
                    filter);
        }
    }

    private void initBugly(Context context) {
        //Bugly
        String channel = CommonUtils.getMetaDataInApplicationTag(context, "CHANNEL_NAME");
        AppConstants.IS_RELEASE_VERSION = !channel.equals(AppConstants.DEVELOP_CHANNEL);
        MLogs.e("IS_RELEASE_VERSION: " + AppConstants.IS_RELEASE_VERSION);

        MLogs.e("versioncode: " + CommonUtils.getCurrentVersionCode(context) + ", versionName:" + CommonUtils.getCurrentVersionName(context));
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        String referChannel = PreferencesUtils.getInstallChannel();
        strategy.setAppChannel(referChannel == null? channel : referChannel);
        CrashReport.initCrashReport(context, "12a06457f1", !AppConstants.IS_RELEASE_VERSION, strategy);
        // close auto report, manual control
        MLogs.e("bugly channel: " + channel + " referrer: "+ referChannel);
        CrashReport.closeCrashReport();
    }


    private class AcivityLifeCycleListener implements ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CustomizeAppData appData = PolestarApp.getApp().getCurrentCustomizeData();
                if (appData != null && DoConfig.get().isHandleInterstitial(null, activity.getClass().getName())) {
                    activity.setTaskDescription(new ActivityManager.TaskDescription(appData.label, appData.getCustomIcon()));
                }
            }
            PolestarApp.getApp().setCurrentAdClone(null, -1);
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}
