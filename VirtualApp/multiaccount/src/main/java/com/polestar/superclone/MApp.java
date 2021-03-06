package com.polestar.superclone;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.widget.Toast;

import com.polestar.ad.AdUtils;
import com.polestar.ad.IAdEventLogger;
import com.polestar.ad.SDKConfiguration;
import com.google.firebase.FirebaseApp;
import com.polestar.clone.CustomizeAppData;
import com.polestar.clone.client.VClientImpl;
import com.polestar.clone.client.core.CrashHandler;
import com.polestar.clone.client.core.InstallStrategy;
import com.polestar.clone.client.core.VirtualCore;
import com.polestar.clone.client.env.SpecialComponentList;
import com.polestar.clone.client.stub.DaemonService;
import com.polestar.clone.client.stub.VASettings;
import com.polestar.clone.remote.InstallResult;
import com.polestar.clone.helper.utils.VLog;
import com.polestar.ad.AdConfig;
import com.polestar.ad.AdConstants;
import com.polestar.ad.adapters.FuseAdLoader;
import com.polestar.billing.BillingProvider;
import com.polestar.superclone.component.AppMonitorService;
import com.polestar.superclone.component.LocalActivityLifecycleCallBacks;
import com.polestar.superclone.component.MComponentDelegate;
import com.polestar.superclone.component.activity.AppStartActivity;
import com.polestar.superclone.component.receiver.PackageChangeReceiver;
import com.polestar.superclone.constant.AppConstants;
import com.polestar.superclone.notification.FastSwitch;
import com.polestar.superclone.reward.AppUser;
import com.polestar.superclone.utils.CommonUtils;
import com.polestar.superclone.utils.MLogs;
import com.polestar.superclone.utils.EventReporter;
import com.polestar.superclone.utils.PreferencesUtils;
import com.polestar.superclone.utils.RemoteConfig;
import com.polestar.superclone.utils.SuperConfig;
import com.polestar.task.database.datamodels.AdTask;
import com.polestar.task.network.Configuration;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


public class MApp extends MultiDexApplication {

    private static MApp gDefault;

    public static MApp getApp() {
        return gDefault;
    }

    static {
        SpecialComponentList.APP_LOADING_ACTIVITY = AppStartActivity.class.getName();
    }
    public static boolean isOpenLog(){
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/polelog");
            boolean ret = file.exists();
            if (ret) {
                Log.d(MLogs.DEFAULT_TAG, "log opened by file");
            }
            return ret;
        }catch (Exception ex){
            return false;
        }
    }

    public static boolean isSupportPkg() {
        return getApp().getPackageName().endsWith("arm64");
    }

    public static boolean isSupportPkgExist() {
        if (isSupportPkg()) {
            return  true;
        } else {
            try{
                ApplicationInfo ai = getApp().getPackageManager().getApplicationInfo(getApp().getPackageName()+ ".arm64",0);
                if (ai != null) {
                    return true;
                }
                ai =  getApp().getPackageManager().getApplicationInfo(AppConstants.SUPPORT_PKG, 0);
                if (ai != null) {
                    return true;
                }
            }catch(Exception ex){

            }
            return false;
        }
    }

    public static boolean needAd() {
        return !(isSupportPkg());
    }

    @Override
    protected void attachBaseContext(Context base) {
        Log.d(MLogs.DEFAULT_TAG, "LIB version: " + com.polestar.clone.BuildConfig.VERSION_NAME + " Type: " + com.polestar.clone.BuildConfig.BUILD_TYPE );

        super.attachBaseContext(base);
        gDefault = this;
        try {
            VASettings.ENABLE_IO_REDIRECT = true;
            VASettings.ENABLE_INNER_SHORTCUT = false;
            VASettings.ENABLE_GMS = PreferencesUtils.isGMSEnable();
            Log.d(MLogs.DEFAULT_TAG, "GMS state: " + VASettings.ENABLE_GMS);
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initAd() {
        SDKConfiguration.Builder builder = new SDKConfiguration.Builder();
        if (!needAd()) {
            for (String s : FuseAdLoader.SUPPORTED_TYPES) {
                builder.disableAdType(s);
            }
        } else {
            builder.mopubAdUnit("f1dcb1c1bd324c23a914a3dd8334bbe6")
                    .admobAppId("ca-app-pub-5490912237269284~7700296749")
                    .ironSourceAppKey("88e91dc5");
        }
        FuseAdLoader.init(new FuseAdLoader.ConfigFetcher() {
            @Override
            public boolean isAdFree(String slot) {
                return PreferencesUtils.isAdFree() && !slot.startsWith(AdTask.TASK_SLOT_PREFIX)
                        && !slot.equals("slot_reward_center_video");
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
//        BatmobiLib.init(gDefault, "8W4OBQJHMXNI1TM9TGZAK4HF");
        //FuseAdLoader.SUPPORTED_TYPES.remove(AdConstants.AdType.AD_SOURCE_FACEBOOK);
        //FuseAdLoader.SUPPORTED_TYPES.remove(AdConstants.AdType.AD_SOURCE_FACEBOOK_INTERSTITIAL);

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
    public void onCreate() {
        super.onCreate();
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                MLogs.d("Main process create");

                FirebaseApp.initializeApp(gDefault);
                RemoteConfig.init();
                initRawData();
                registerActivityLifecycleCallbacks(new LocalActivityLifecycleCallBacks(MApp.this, true));
                EventReporter.init(gDefault);
                SuperConfig.get();
                if (!isSupportPkg()) {
                    BillingProvider.get().updateStatus(null);
                }
                initAd();

                if (AppUser.check() && AppUser.isRewardEnabled()) {
                    Configuration.URL_PREFIX = RemoteConfig.getString("config_task_server");
                    Configuration.APP_VERSION_CODE = BuildConfig.VERSION_CODE;
                    Configuration.PKG_NAME = BuildConfig.APPLICATION_ID;
                    FuseAdLoader.setUserId(AppUser.getInstance().getMyId());
                    AppUser.getInstance().preloadRewardVideoTask();
                }
                //Do some ad preload
                AppStartActivity.preloadAd(gDefault);
                AppMonitorService.preloadCoverAd(null, -1);
                initReceiver();
                if (FastSwitch.isEnable()) {
                    FastSwitch.getInstance(gDefault).init();
                }
            }

            @Override
            public void onVirtualProcess() {
                MLogs.d("Virtual process create");
                SuperConfig.get();
                MComponentDelegate delegate = new MComponentDelegate();
                delegate.asyncInit();
                virtualCore.setComponentDelegate(delegate);

                virtualCore.setAppApiDelegate(new AppApiDelegate());
            }

            @Override
            public void onServerProcess() {
                MLogs.d("Server process create");
                VirtualCore.get().setAppRequestListener(new VirtualCore.AppRequestListener() {
                    @Override
                    public void onRequestInstall(String path) {
                        //We can start AppInstallActivity TODO
                        Toast.makeText(MApp.this, "Installing: " + path, Toast.LENGTH_SHORT).show();
                        InstallResult res = VirtualCore.get().installPackage("", path, InstallStrategy.UPDATE_IF_EXIST);
                        if (res.isSuccess) {
                            try {
                                VirtualCore.get().preOpt(res.packageName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (res.isUpdate) {
                                Toast.makeText(MApp.this, "Update: " + res.packageName + " success!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MApp.this, "Install: " + res.packageName + " success!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MApp.this, "Install failed: " + res.error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRequestUninstall(String pkg) {
                        Toast.makeText(MApp.this, "Uninstall: " + pkg, Toast.LENGTH_SHORT).show();

                    }
                });
                FirebaseApp.initializeApp(gDefault);
                RemoteConfig.init();
                SuperConfig.get();
                MComponentDelegate delegate = new MComponentDelegate();
                delegate.asyncInit();
                VirtualCore.get().setComponentDelegate(delegate);
                initAd();
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

        if (isOpenLog() || !AppConstants.IS_RELEASE_VERSION ) {
            VLog.openLog();
            VLog.d(MLogs.DEFAULT_TAG, "VLOG is opened");
            MLogs.DEBUG = true;
            AdConstants.DEBUG = true;
        }
        VLog.setKeyLogger(new VLog.IKeyLogger() {
            @Override
            public void keyLog(Context context, String tag, String log) {
                MLogs.logBug(tag,log);
                EventReporter.keyLog(MApp.gDefault, tag, log);
            }

            @Override
            public void logBug(String tag, String log) {
                MLogs.logBug(tag, log);
            }
        });
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
            crash.setPackage(BuildConfig.APPLICATION_ID);
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

    private void initRawData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String localFilePath = getApplicationContext().getFilesDir().toString();
                String path = localFilePath + "/" + AppConstants.POPULAR_FILE_NAME;
                copyRawDataToLocal(path, R.raw.popular_apps);
            }
        }).start();
    }

    private void copyRawDataToLocal(String filePath, int resourceId) {
        try {
            File file = new File(filePath);
            // already copied
            if (file.exists()) {
                return;
            } else {
                if (file.createNewFile()) {
                    InputStream in = getResources().openRawResource(resourceId);
                    OutputStream out = new FileOutputStream(file);
                    byte[] buff = new byte[4096];
                    int count = 0;
                    while ((count = in.read(buff)) > 0) {
                        out.write(buff, 0, count);
                    }
                    out.close();
                    in.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        CrashReport.initCrashReport(context, "f3b4dfc65f", !AppConstants.IS_RELEASE_VERSION, strategy);
        // close auto report, manual control
        MLogs.e("bugly channel: " + channel + " referrer: "+ referChannel);
        CrashReport.closeCrashReport();
    }
}

