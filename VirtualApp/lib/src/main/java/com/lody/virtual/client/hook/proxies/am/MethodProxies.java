package com.lody.virtual.client.hook.proxies.am;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.IServiceConnection;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.system.Os;
import android.text.TextUtils;
import android.util.TypedValue;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.hook.secondary.ServiceConnectionDelegate;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.ActivityClientRecord;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.ChooserActivity;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.client.stub.StubPendingActivity;
import com.lody.virtual.client.stub.StubPendingReceiver;
import com.lody.virtual.client.stub.StubPendingService;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.DrawableUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.server.am.BroadcastSystem;
import com.lody.virtual.server.interfaces.IAppRequestListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApk;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.IIntentReceiverJB;
import mirror.android.content.pm.UserInfo;

import static android.os.Build.VERSION_CODES.N;

/**
 * @author Lody
 */
@SuppressWarnings("unused")
class MethodProxies {


    static class ForceStopPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "forceStopPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            int userId = VUserHandle.myUserId();
            VActivityManager.get().killAppByPkg(pkg, userId);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class CrashApplication extends MethodProxy {

        @Override
        public String getMethodName() {
            return "crashApplication";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class AddPackageDependency extends MethodProxy {

        @Override
        public String getMethodName() {
            return "addPackageDependency";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GetPackageForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            String pkg = VActivityManager.get().getPackageForToken(token);
            if (pkg != null) {
                return pkg;
            }
            return super.call(who, method, args);
        }
    }

    static class UnbindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IServiceConnection conn = (IServiceConnection) args[0];
            ServiceConnectionDelegate delegate = ServiceConnectionDelegate.removeDelegate(conn);
            if (delegate == null) {
                return method.invoke(who, args);
            }
            return VActivityManager.get().unbindService(delegate);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class GetContentProviderExternal extends GetContentProvider {

        @Override
        public String getMethodName() {
            return "getContentProviderExternal";
        }

        @Override
        public int getProviderNameIndex() {
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StartVoiceActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startVoiceActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class UnstableProviderDied extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unstableProviderDied";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] == null) {
                return 0;
            }
            return method.invoke(who, args);
        }
    }


    static class PeekService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "peekService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceLastAppPkg(args);
            Intent service = (Intent) args[0];
            String resolvedType = (String) args[1];
            return VActivityManager.get().peekService(service, resolvedType);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String creator = (String) args[1];
            args[1] = getHostPkg();
            String[] resolvedTypes = (String[]) args[6];
            int type = (int) args[0];
            int flags = (int) args[7];
            int indexToken = ArrayUtils.indexOfFirst(args, IBinder.class);
            IBinder token = indexToken == -1 ? null : (IBinder) args[indexToken];
            if ((PendingIntent.FLAG_UPDATE_CURRENT & flags) != 0) {
                flags = (flags & ~(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_NO_CREATE)) | PendingIntent.FLAG_CANCEL_CURRENT;
            }
            if (args[5] instanceof Intent[]) {
                Intent[] intents = (Intent[]) args[5];
                if (intents.length > 0) {
                    Intent intent = intents[intents.length - 1];
                    if (resolvedTypes != null && resolvedTypes.length > 0) {
                        intent.setDataAndType(intent.getData(), resolvedTypes[resolvedTypes.length - 1]);
                    }
                    Intent targetIntent = redirectIntentSender(type, creator, intent, token);
                    if (targetIntent != null) {
                        intents[intents.length - 1] = targetIntent;
                    }
                }
            }
            args[7] = flags;
            args[1] = getHostPkg();
            // Force userId to 0
            if (args[args.length - 1] instanceof Integer) {
                args[args.length - 1] = 0;
            }
            IInterface sender = (IInterface) method.invoke(who, args);
            if (sender != null && creator != null) {
                VActivityManager.get().addPendingIntent(sender.asBinder(), creator);
            }
            return sender;
        }

        private Intent redirectIntentSender(int type, String creator, Intent intent, IBinder token) {
            Intent newIntent = intent.cloneFilter();
            boolean ok = false;
            switch (type) {
                case ActivityManagerCompat.INTENT_SENDER_ACTIVITY: {
                    VLog.d(getMethodName(), "INTENT_SENDER_ACTIVITY " + intent.toString());
                    ComponentInfo info = VirtualCore.get().resolveActivityInfo(intent, VUserHandle.myUserId());
                    if (info != null) {
                        ok = true;
                        newIntent.setClass(getHostContext(), StubPendingActivity.class);
                        //newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        newIntent.setFlags(intent.getFlags());
                        if (token != null) {
                            VLog.d(getMethodName(), "token not null");
                            try {
                                ComponentName componentName = VActivityManager.get().getActivityForToken(token);
                                if (componentName != null) {
                                    VLog.d(getMethodName(), "component " + componentName.toString());
                                    newIntent.putExtra("_VA_|_caller_", componentName);
                                }
                            }catch (Exception e){
                                VLog.logbug(getMethodName(), VLog.getStackTraceString(e));
                            }
                        }
                    }
                }
                break;
                case ActivityManagerCompat.INTENT_SENDER_SERVICE: {
                    ComponentInfo info = VirtualCore.get().resolveServiceInfo(intent, VUserHandle.myUserId());
                    if (info != null) {
                        ok = true;
                        newIntent.setClass(getHostContext(), StubPendingService.class);
                    }
                }
                break;
                case ActivityManagerCompat.INTENT_SENDER_BROADCAST: {
                    ok = true;
                    newIntent.setClass(getHostContext(), StubPendingReceiver.class);
                }
                break;
                default:
                    return null;
            }
            if (!ok) {
                return null;
            }
            newIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());
            newIntent.putExtra("_VA_|_intent_", intent);
            newIntent.putExtra("_VA_|_creator_", creator);
            newIntent.putExtra("_VA_|_from_inner_", true);
            return newIntent;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class StartActivity extends MethodProxy {

        private static final String SCHEME_FILE = "file";
        private static final String SCHEME_PACKAGE = "package";

        @Override
        public String getMethodName() {
            return "startActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int intentIndex = ArrayUtils.indexOfObject(args, Intent.class, 1);
            if (intentIndex < 0) {
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
            int callerIndex = ArrayUtils.indexOfObject(args, IBinder.class, 1);
            IBinder caller = callerIndex >= 0 ? (IBinder) args[callerIndex] : null;;
            int resultToIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            String resolvedType = (String) args[intentIndex + 1];
            Intent intent = (Intent) args[intentIndex];
            intent.setDataAndType(intent.getData(), resolvedType);
            IBinder resultTo = resultToIndex >= 0 ? (IBinder) args[resultToIndex] : null;
            int userId = VUserHandle.myUserId();

            //Work around fb ad icon click
            VLog.d("StartActivity", "intent: " + intent.toString());
            if(VirtualCore.get().isServerProcess() &&
                    Intent.ACTION_VIEW.equals(intent.getAction()) &&
                    intent.getDataString() != null &&
                    intent.getDataString().contains("https://m.facebook.com/")) {
                return method.invoke(who, args);
            }
            if (ComponentUtils.isStubComponent(intent)) {
                return method.invoke(who, args);
            }

            if (Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())
                    || (Intent.ACTION_VIEW.equals(intent.getAction())
                    && "application/vnd.android.package-archive".equals(intent.getType()))) {
                if (handleInstallRequest(intent)) {
                    return 0;
                }
            } else if ((Intent.ACTION_UNINSTALL_PACKAGE.equals(intent.getAction())
                    || Intent.ACTION_DELETE.equals(intent.getAction()))
                    && "package".equals(intent.getScheme())) {

                if (handleUninstallRequest(intent)) {
                    return 0;
                }
            }

            String resultWho = null;
            int requestCode = 0;
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            if (resultTo != null) {
                resultWho = (String) args[resultToIndex + 1];
                requestCode = (int) args[resultToIndex + 2];
            }
            // chooser
            if (ChooserActivity.check(intent)) {
                intent.setComponent(new ComponentName(getHostContext(), ChooserActivity.class));
                intent.putExtra(Constants.EXTRA_USER_HANDLE, userId);
                intent.putExtra(ChooserActivity.EXTRA_DATA, options);
                intent.putExtra(ChooserActivity.EXTRA_WHO, resultWho);
                intent.putExtra(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
                return method.invoke(who, args);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                args[intentIndex - 1] = getHostPkg();
            }

            ActivityInfo activityInfo = VirtualCore.get().resolveActivityInfo(intent, userId);
            if (activityInfo == null) {
                VLog.e("VActivityManager", "Unable to resolve activityInfo : " + intent);
                if (intent.getPackage() != null && isAppPkg(intent.getPackage())) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }
                return method.invoke(who, args);
            }
            int res = VActivityManager.get().startActivity(intent, activityInfo, resultTo, options, resultWho, requestCode, VUserHandle.myUserId());
            if (res != 0 && resultTo != null && requestCode > 0) {
                VActivityManager.get().sendActivityResult(resultTo, resultWho, requestCode);
            }
            if (resultTo != null) {
                ActivityClientRecord r = VActivityManager.get().getActivityRecord(resultTo);
                if (r != null && r.activity != null) {
                    try {
                        TypedValue out = new TypedValue();
                        Resources.Theme theme = r.activity.getResources().newTheme();
                        theme.applyStyle(activityInfo.getThemeResource(), true);
                        if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                            TypedArray array = theme.obtainStyledAttributes(out.data,
                                    new int[]{
                                            android.R.attr.activityOpenEnterAnimation,
                                            android.R.attr.activityOpenExitAnimation
                                    });

                            r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                            array.recycle();
                        }
                    } catch (Throwable e) {
                        // Ignore
                    }
                }
            }
            return res;
        }


        private boolean handleInstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_FILE.equals(packageUri.getScheme())) {
                    File sourceFile = new File(packageUri.getPath());
                    try {
                        listener.onRequestInstall(sourceFile.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

        private boolean handleUninstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    String pkg = packageUri.getSchemeSpecificPart();
                    try {
                        listener.onRequestUninstall(pkg);
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

    }

    static class StartActivities extends MethodProxy {

        @Override
        public String getMethodName() {
            return "startActivities";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent[] intents = ArrayUtils.getFirst(args, Intent[].class);
            String[] resolvedTypes = ArrayUtils.getFirst(args, String[].class);
            IBinder token = null;
            int tokenIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            if (tokenIndex != -1) {
                token = (IBinder) args[tokenIndex];
            }
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);

            return VActivityManager.get().startActivities(intents, resolvedTypes, token, options, VUserHandle.myUserId());
        }
    }


    static class FinishActivity extends MethodProxy {
        @Override
        public String getMethodName() {
            return "finishActivity";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            IBinder token = (IBinder) args[0];
            ActivityClientRecord r = VActivityManager.get().getActivityRecord(token);
            boolean taskRemoved = VActivityManager.get().onActivityDestroy(token);
            if (!taskRemoved && r != null && r.activity != null && r.info.getThemeResource() != 0) {
                try {
                    TypedValue out = new TypedValue();
                    Resources.Theme theme = r.activity.getResources().newTheme();
                    theme.applyStyle(r.info.getThemeResource(), true);
                    if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                        TypedArray array = theme.obtainStyledAttributes(out.data,
                                new int[]{
                                        android.R.attr.activityCloseEnterAnimation,
                                        android.R.attr.activityCloseExitAnimation
                                });
                        r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                        array.recycle();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return super.afterCall(who, method, args, result);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCallingPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingPackage(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageForIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getPackageForIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface sender = (IInterface) args[0];
            if (sender != null) {
                String packageName = VActivityManager.get().getPackageForIntentSender(sender.asBinder());
                if (packageName != null) {
                    return packageName;
                }
            }
            return super.call(who, method, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    @SuppressWarnings("unchecked")
    static class PublishContentProviders extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishContentProviders";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetServices extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getServices";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int maxNum = (int) args[0];
            int flags = (int) args[1];
            return VActivityManager.get().getServices(maxNum, flags).getList();
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GrantUriPermissionFromOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "grantUriPermissionFromOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class SetServiceForeground extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setServiceForeground";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class UpdateDeviceOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "updateDeviceOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class GetIntentForIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentForIntentSender";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            Intent intent = (Intent) super.afterCall(who, method, args, result);
            try {
                if (intent != null && intent.hasExtra("_VA_|_intent_")) {
                    return intent.getParcelableExtra("_VA_|_intent_");
                }
            }catch (Exception e){
                VLog.logbug("getIntentForIntentSender", VLog.getStackTraceString(e));
            }
            return intent;
        }
    }


    static class UnbindFinished extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindFinished";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            Intent service = (Intent) args[1];
            boolean doRebind = (boolean) args[2];
            VActivityManager.get().unbindFinished(token, service, doRebind);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StartActivityIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "startActivityIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            return super.call(who, method, args);
        }
    }


    static class BindService extends MethodProxy {
        private static final HashSet BLOCK_ACTION_LIST = new HashSet();
        private static final HashSet BLOCK_COMPONENT_LIST = new HashSet();
        private static final String TAG = "BindService";
        static {
            BLOCK_ACTION_LIST.add("com.android.vending.contentfilters.IContentFiltersService.BIND");
        }

        static {
            BLOCK_COMPONENT_LIST.add("com.google.android.finsky.contentfilter.impl.ContentFiltersService");
        }

        @Override
        public String getMethodName() {
            return "bindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            IBinder token = (IBinder) args[1];
            Intent service = (Intent) args[2];
            String resolvedType = (String) args[3];
            IServiceConnection conn = (IServiceConnection) args[4];
            int flags = (int) args[5];
            int userId = VUserHandle.myUserId();
            if (isServerProcess()) {
                userId = service.getIntExtra("_VA_|_user_id_", VUserHandle.USER_NULL);
            }
            if (service != null && BLOCK_ACTION_LIST.contains(service.getAction())) {
                VLog.logbug(TAG, "action is blocked: " + service.getAction());
                return 0;
            }
            if (userId == VUserHandle.USER_NULL) {
                VLog.logbug(TAG, "userid is " + userId);
                return method.invoke(who, args);
            }
            VLog.d(TAG, "for intent: " + service);
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, userId);
            if (serviceInfo != null) {
                if(BLOCK_COMPONENT_LIST.contains(serviceInfo.name)) {
                    VLog.logbug(TAG, "component is blocked: " + serviceInfo.name);
                    return 0;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                }
                conn = ServiceConnectionDelegate.getDelegate(conn);
                return VActivityManager.get().bindService(caller.asBinder(), token, service, resolvedType,
                        conn, flags, userId);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }


    static class StartService extends MethodProxy {
        private static final HashSet BLOCK_ACTION_LIST = new HashSet();
        private static final HashSet BLOCK_COMPONENT_LIST = new HashSet();
        private static final String TAG = "StartService";

        static {
            BLOCK_ACTION_LIST.add("com.google.android.gms.chimera.container.LOG_LOAD_ATTEMPT");
            BLOCK_ACTION_LIST.add("com.android.vending.contentfilters.IContentFiltersService.BIND");
            BLOCK_ACTION_LIST.add("com.google.android.chimera.FileApkManager.DELETE_UNUSED_FILEAPKS");
        }

        static {
            BLOCK_COMPONENT_LIST.add("com.google.android.finsky.contentfilter.impl.ContentFiltersService");
            BLOCK_COMPONENT_LIST.add("com.google.android.gsf.update.SystemUpdateService");
            //BLOCK_COMPONENT_LIST.add("com.google.android.finsky.wear.WearSupportService");
        }

        @Override
        public String getMethodName() {
            return "startService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface appThread = (IInterface) args[0];
            Intent service = (Intent) args[1];
            String resolvedType = (String) args[2];
            if(service!=null) {
                VLog.d(TAG, "intent: " + service.toString());
                if (service.getComponent() != null) {
                    VLog.d(TAG, " " + service.getComponent().getClassName() );
                }
                if (service.getComponent() != null && service.getComponent().getClassName().equals("com.qihoo.ls.SoService")){
                    return  null;
                }
                if (BLOCK_ACTION_LIST.contains(service.getAction())) {
                    VLog.logbug(TAG, "action is blocked: " + service);
                    return null;
                }
            }
            if (service.getComponent() != null
                    && (!service.getComponent().getClassName().equals(StubPendingService.class.getName()))
                    && getHostPkg().equals(service.getComponent().getPackageName())) {
                // for server process
                return method.invoke(who, args);
            }
            int userId = VUserHandle.myUserId();
            boolean fromInner = false;
            try{
                fromInner = service.getBooleanExtra("_VA_|_from_inner_", false);
            }catch (Exception e){
                VLog.logbug("StartService", VLog.getStackTraceString(e));
            }
            if (fromInner) {
                service = service.getParcelableExtra("_VA_|_intent_");
                userId = service.getIntExtra("_VA_|_user_id_", userId);
            } else {
                if (isServerProcess()) {
                    userId = service.getIntExtra("_VA_|_user_id_", VUserHandle.USER_NULL);
                }
            }
            service.setDataAndType(service.getData(), resolvedType);
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, VUserHandle.myUserId());
            if (serviceInfo != null) {
                if (BLOCK_COMPONENT_LIST.contains(serviceInfo.name)){
                    VLog.logbug(TAG, "Blocked component: " + serviceInfo.name);
                    return null;
                }
                return VActivityManager.get().startService(appThread, service, resolvedType, userId);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class StartActivityAndWait extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityAndWait";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class PublishService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            Intent intent = (Intent) args[1];
            IBinder service = (IBinder) args[2];
            VActivityManager.get().publishService(token, intent, service);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    @SuppressWarnings("unchecked")
    static class GetRunningAppProcesses extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getRunningAppProcesses";
        }

        @Override
        public synchronized Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningAppProcessInfo> infoList = (List<ActivityManager.RunningAppProcessInfo>) method
                    .invoke(who, args);
            if (infoList != null) {
                for (ActivityManager.RunningAppProcessInfo info : infoList) {
                    if (VActivityManager.get().isAppPid(info.pid)) {
                        List<String> pkgList = VActivityManager.get().getProcessPkgList(info.pid);
                        String processName = VActivityManager.get().getAppProcessName(info.pid);
                        if (processName != null) {
                            info.processName = processName;
                        }
                        if (pkgList != null) {
                            info.pkgList = pkgList.toArray(new String[pkgList.size()]);
                        }
                        info.uid = VUserHandle.getAppId(VActivityManager.get().getUidByPid(info.pid));
                    }
                }
            }
            return infoList;
        }
    }


    static class SetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCallingActivity extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingActivity(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCurrentUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCurrentUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            try {
                return UserInfo.ctor.newInstance(0, "user", VUserInfo.FLAG_PRIMARY);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    static class KillApplicationProcess extends MethodProxy {

        @Override
        public String getMethodName() {
            return "killApplicationProcess";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args.length > 1 && args[0] instanceof String && args[1] instanceof Integer) {
                String processName = (String) args[0];
                int uid = (int) args[1];
                VActivityManager.get().killApplicationProcess(processName, uid);
                return 0;
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class StartActivityAsUser extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class CheckPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String permission = (String) args[0];
            if (SpecialComponentList.isWhitePermission(permission)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            if (permission.startsWith("com.google")) {
                return PackageManager.PERMISSION_GRANTED;
            }
            args[args.length - 1] = getRealUid();
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class StartActivityAsCaller extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsCaller";
        }
    }


    static class HandleIncomingUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "handleIncomingUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int lastIndex = args.length - 1;
            if (args[lastIndex] instanceof String) {
                args[lastIndex] = getHostPkg();
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    @SuppressWarnings("unchecked")
    static class GetTasks extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getTasks";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = (List<ActivityManager.RunningTaskInfo>) method
                    .invoke(who, args);
            for (ActivityManager.RunningTaskInfo info : runningTaskInfos) {
                AppTaskInfo taskInfo = VActivityManager.get().getTaskInfo(info.id);
                if (taskInfo != null) {
                    info.topActivity = taskInfo.topActivity;
                    info.baseActivity = taskInfo.baseActivity;
                }
            }
            return runningTaskInfos;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPersistedUriPermissions extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPersistedUriPermissions";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class RegisterReceiver extends MethodProxy {
        private static final String TAG = "RegisterReceiver";
        private static final int IDX_IIntentReceiver = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 2
                : 1;

        private static final int IDX_RequiredPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 4
                : 3;
        private static final int IDX_IntentFilter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 3
                : 2;

        private WeakHashMap<IBinder, IIntentReceiver> mProxyIIntentReceivers = new WeakHashMap<>();

        @Override
        public String getMethodName() {
            return "registerReceiver";
        }

        private boolean  isSticky(IntentFilter filter) {
            if (filter != null) {
                Iterator<String> iterator = filter.actionsIterator();
                while (iterator.hasNext()) {
                    String action = iterator.next();
                    if (SpecialComponentList.isSticky(action)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String origPkg = MethodParameterUtils.replaceFirstAppPkg(args);
            Object directRet = null;
            args[IDX_RequiredPermission] = null;
            if (args.length > IDX_IIntentReceiver && args[IDX_IIntentReceiver] == null) {
                VLog.logbug(TAG, "null receiver: " );
                return method.invoke(who,args);
            }
            IntentFilter filter = (IntentFilter) args[IDX_IntentFilter];
            if (isSticky(filter)) {
                Object origReceiver = args[IDX_IIntentReceiver];
                args[IDX_IIntentReceiver] = null;
                directRet = method.invoke(who,args);
                args[IDX_IIntentReceiver] = origReceiver;
            }
            SpecialComponentList.protectIntentFilter(filter, origPkg);
            if (args.length > IDX_IIntentReceiver && IIntentReceiver.class.isInstance(args[IDX_IIntentReceiver])) {
                final IInterface old = (IInterface) args[IDX_IIntentReceiver];
                if (!IIntentReceiverProxy.class.isInstance(old)) {
                    final IBinder token = old.asBinder();
                    if (token != null) {
                        token.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                token.unlinkToDeath(this, 0);
                                mProxyIIntentReceivers.remove(token);
                            }
                        }, 0);
                        IIntentReceiver proxyIIntentReceiver = mProxyIIntentReceivers.get(token);
                        if (proxyIIntentReceiver == null) {
                            proxyIIntentReceiver = new IIntentReceiverProxy(old);
                            mProxyIIntentReceivers.put(token, proxyIIntentReceiver);
                        }
                        WeakReference mDispatcher = LoadedApk.ReceiverDispatcher.InnerReceiver.mDispatcher.get(old);
                        if (mDispatcher != null) {
                            LoadedApk.ReceiverDispatcher.mIIntentReceiver.set(mDispatcher.get(), proxyIIntentReceiver);
                            args[IDX_IIntentReceiver] = proxyIIntentReceiver;
                        }
                    }
                }
            }
            if (directRet != null) {
                return directRet;
            } else {
                return method.invoke(who, args);
            }
        }


        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

        private static class IIntentReceiverProxy extends IIntentReceiver.Stub {

            IInterface mOld;

            IIntentReceiverProxy(IInterface old) {
                this.mOld = old;
            }

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky, int sendingUser) throws RemoteException {
                if (!accept(intent)) {
                    return;
                }
                try {
                    if (intent.hasExtra("_VA_|_intent_")) {
                        intent = intent.getParcelableExtra("_VA_|_intent_");
                    }
                }catch (Exception e ) {
                    VLog.logbug(TAG, VLog.getStackTraceString(e));
                }
                SpecialComponentList.unprotectIntent(intent);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    IIntentReceiverJB.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky, sendingUser);
                } else {
                    mirror.android.content.IIntentReceiver.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky);
                }
            }

            private boolean accept(Intent intent) {
                int uid = intent.getIntExtra("_VA_|_uid_", -1);
                int userId = intent.getIntExtra("_VA_|_user_id_", -1);
                VLog.d("RegisterReceiver", "Accept uid " + uid + " userid:"+userId
                        + " vuid:"+VClientImpl.get().getVUid() + " myuserId: " + VUserHandle.myUserId());
                if (uid != -1) {
                    return VClientImpl.get().getVUid() == uid;
                }
                return userId == -1 || userId == VUserHandle.myUserId();
            }

            @SuppressWarnings("unused")
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky) throws RemoteException {
                this.performReceive(intent, resultCode, data, extras, ordered, sticky, 0);
            }

        }
    }


    static class StopService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            Intent intent = (Intent) args[1];
            String resolvedType = (String) args[2];
            intent.setDataAndType(intent.getData(), resolvedType);
            ComponentName componentName = intent.getComponent();
            PackageManager pm = VirtualCore.getPM();
            if (componentName == null) {
                ResolveInfo resolveInfo = pm.resolveService(intent, 0);
                if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                    componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                }
            }
            if (componentName != null && !getHostPkg().equals(componentName.getPackageName())) {
                return VActivityManager.get().stopService(caller, intent, resolvedType);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class RefContentProvider extends MethodProxy {
        private final static String TAG = "RefContentProvider";
        @Override
        public String getMethodName() {
            return "refContentProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] == null) {
                VLog.logbug(TAG, "connection is null. return");
                return false;
            }else{
                try{
                    return method.invoke(who, args);
                }catch (InvocationTargetException e){
                    VLog.logbug(TAG, VLog.getStackTraceString(e));
                    return false;
                }
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class RemoveContentProvider extends MethodProxy {
        private final static String TAG = "RemoveContentProvider";

        @Override
        public String getMethodName() {
            return "removeContentProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] == null) {
                VLog.logbug(TAG, "connection is null. return");
                return null;
            }else{
                try{
                    return method.invoke(who, args);
                }catch (InvocationTargetException e){
                    VLog.logbug(TAG, VLog.getStackTraceString(e));
                    return null;
                }
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GetContentProvider extends MethodProxy {
        private static final String TAG = "GetContentProvider";
        @Override
        public String getMethodName() {
            return "getContentProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int nameIdx = getProviderNameIndex();
            String name = (String) args[nameIdx];
            int userId = VUserHandle.myUserId();
            ProviderInfo info = VPackageManager.get().resolveContentProvider(name, 0, userId);
            if (info != null && info.enabled && isAppPkg(info.packageName)) {
                int targetVPid = VActivityManager.get().initProcess(info.packageName, info.processName, userId);
                if (targetVPid == -1) {
                    return null;
                }
                args[nameIdx] = StubManifest.getStubAuthority(targetVPid);
                Object holder = method.invoke(who, args);
                if (holder == null) {
                    VLog.logbug(TAG, "holder == null " + name + " pkg: " + info.packageName);
                    return null;
                }
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    if (provider != null) {
                        provider = VActivityManager.get().acquireProviderClient(userId, info);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                    ContentProviderHolderOreo.info.set(holder, info);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    if (provider != null) {
                        provider = VActivityManager.get().acquireProviderClient(userId, info);
                        VLog.logbug(TAG, "provider != null " + name + " process: " + info.processName +
                                " pkg: " + info.packageName + " provider " + provider);
                        if (provider == null) {
                            provider = VActivityManager.get().acquireProviderClient(userId, info);
                            VLog.logbug(TAG, "retry result: " + provider);
                        }
                    } else {
                        VLog.logbug(TAG, "provider == null " + name + " process: " + info.processName +
                                " pkg: " + info.packageName + " current: " + VClientImpl.get().getCurrentPackage());
                        if (! info.packageName.equals(VClientImpl.get().getCurrentPackage())) {
                            provider = VActivityManager.get().acquireProviderClient(userId, info);
                        }
                        VLog.logbug(TAG, "provider result " + provider);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                    IActivityManager.ContentProviderHolder.info.set(holder, info);
                }
                return holder;
            }
            Object holder = method.invoke(who, args);
            if (holder != null) {
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    info = ContentProviderHolderOreo.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    info = IActivityManager.ContentProviderHolder.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
                return holder;
            }
            return null;
        }


        public int getProviderNameIndex() {
            return 1;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class SetTaskDescription extends MethodProxy {
        @Override
        public String getMethodName() {
            return "setTaskDescription";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ActivityManager.TaskDescription td = (ActivityManager.TaskDescription)args[1];
            String label = td.getLabel();
            Bitmap icon = td.getIcon();
            String VACLIENT_SUFFIX = " Cloned";
//		if ((label == null || !label.startsWith(VACLIENT_SUFFIX) || icon == null)){
            Application app = VClientImpl.get().getCurrentApplication();
            if (app != null) {
                if (label == null) {
                    label = "" + app.getApplicationInfo().loadLabel(app.getPackageManager());
                }

                if (VirtualCore.get().getAppApiDelegate() != null) {
                    icon = VirtualCore.get().getAppApiDelegate().createCloneTagedBitmap(app.getPackageName(), icon);
                    label = VirtualCore.get().getAppApiDelegate().getCloneTagedLabel(label);
                }

                if (icon == null) {
                    Drawable drawable = app.getApplicationInfo().loadIcon(app.getPackageManager());
                    if (drawable != null) {
                        icon = DrawableUtils.drawableToBitMap(drawable);
                    }
                }
                args[1] = new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
            }
//		}
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StopServiceToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopServiceToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int startId = (int) args[2];
            if (componentName != null) {
                return VActivityManager.get().stopServiceToken(componentName, token, startId);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class StartActivityWithConfig extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityWithConfig";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }

    static class StartNextMatchingActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startNextMatchingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return false;
        }
    }


    static class BroadcastIntent extends MethodProxy {

        private static final HashSet ACTION_BLACK_LIST = new HashSet<String>();

        static {
            ACTION_BLACK_LIST.add("com.google.android.gms.walletp2p.phenotype.ACTION_PHENOTYPE_REGISTER");
            ACTION_BLACK_LIST.add("com.facebook.zero.ACTION_ZERO_REFRESH_TOKEN");
            ACTION_BLACK_LIST.add("com.google.android.gms.magictether.SCANNED_DEVICE");
        }

        @Override
        public String getMethodName() {
            return "broadcastIntent";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent intent = (Intent) args[1];
            String type = (String) args[2];
            if (intent == null || intent.getAction() == null) {
                return 0;
            }
            //FB will send it when first user login
            if (ACTION_BLACK_LIST.contains(intent.getAction())) {
                VLog.logbug("BroadcastIntent", "action is blocked " + intent);
                return  0;
            }
            if (intent.getAction().equals("appclone.intent.action.SHOW_CRASH_DIALOG")) {
                return method.invoke(who, args);
            }
            intent.setDataAndType(intent.getData(), type);
            if (VirtualCore.get().getComponentDelegate() != null) {
                VirtualCore.get().getComponentDelegate().onSendBroadcast(intent);
            }
            Intent newIntent = handleIntent(intent);
            if (newIntent != null) {
                args[1] = newIntent;
            } else {
                return 0;
            }

            if (args[7] instanceof String || args[7] instanceof String[]) {
                // clear the permission
                args[7] = null;
            }
            return method.invoke(who, args);
        }


        private Intent handleIntent(final Intent intent) {
            final String action = intent.getAction();
            if ("android.intent.action.CREATE_SHORTCUT".equals(action)
                    || "com.android.launcher.action.INSTALL_SHORTCUT".equals(action)) {
                handleInstallShortcutIntent(intent);
            } else if ("com.android.launcher.action.UNINSTALL_SHORTCUT".equals(action)) {
                handleUninstallShortcutIntent(intent);
            } else {
                return ComponentUtils.redirectBroadcastIntent(intent, VUserHandle.myUserId());
            }
            return intent;
        }

        private void handleInstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            String pkg;
            if (shortcut != null) {
                ComponentName component = shortcut.resolveActivity(VirtualCore.getPM());
                if (component != null) {
                    pkg = component.getPackageName();
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    newShortcutIntent.putExtra("_VA_|_intent_", shortcut);
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());
                    intent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);
                    if (VirtualCore.get().getAppApiDelegate() != null) {
                        String label = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                        label = VirtualCore.get().getAppApiDelegate().getCloneTagedLabel(label);
                        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
                    }
                    Intent.ShortcutIconResource icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (icon != null && !TextUtils.equals(icon.packageName, getHostPkg())) {
                        try {
                            Resources resources = VirtualCore.get().getResources(pkg);
                            if (resources != null) {
                                int resId = resources.getIdentifier(icon.resourceName, "drawable", pkg);
                                if (resId > 0) {
                                    Drawable iconDrawable = resources.getDrawable(resId);
                                    Bitmap newIcon = BitmapUtils.drawableToBitmap(iconDrawable);
                                    newIcon = VirtualCore.get().getAppApiDelegate().createCloneTagedBitmap(icon.packageName, newIcon);
                                    if (newIcon != null) {
                                        intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newIcon);
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Bitmap origIcon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                            if (origIcon != null && pkg != null && !TextUtils.equals(pkg, getHostPkg())){
                                Bitmap newIcon = VirtualCore.get().getAppApiDelegate().createCloneTagedBitmap(pkg, origIcon);
                                if (newIcon != null) {
                                    intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON);
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newIcon);
                                }
                            }
                        }catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private void handleUninstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName componentName = shortcut.resolveActivity(getPM());
                if (componentName != null) {
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);
                }
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetActivityClassForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getActivityClassForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getActivityForToken(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class CheckGrantUriPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkGrantUriPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class ServiceDoneExecuting extends MethodProxy {

        @Override
        public String getMethodName() {
            return "serviceDoneExecuting";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int type = (int) args[1];
            int startId = (int) args[2];
            int res = (int) args[3];
            VActivityManager.get().serviceDoneExecuting(token, type, startId, res);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }
}
