package com.polestar.clone.client;

import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.os.Binder;
import android.os.Build;
import android.os.Process;

import com.polestar.clone.client.core.VirtualCore;
import com.polestar.clone.client.env.VirtualRuntime;
import com.polestar.clone.client.ipc.VActivityManager;
import com.polestar.clone.helper.utils.VLog;
import com.polestar.clone.os.VUserHandle;
import com.polestar.clone.remote.InstalledAppInfo;
import com.polestar.clone.StubService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;

/**
 * VirtualApp Native Project
 */
public class NativeEngine {

	private static final String TAG = NativeEngine.class.getSimpleName();

    private static Map<String, InstalledAppInfo> sDexOverrideMap;
    private static Method gOpenDexFileNative;
    private static Method gCameraNativeSetup;
    private static int gCameraMethodType;
    private static Method gAudioRecordNativeCheckPermission;
    private static boolean sFlag;

    static {
        try {
            System.loadLibrary("spc-native");
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    static {
        String methodName =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? "openDexFileNative" : "openDexFile";
        for (Method method : DexFile.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                gOpenDexFileNative = method;
                break;
            }
        }
        if (gOpenDexFileNative == null) {
            throw new RuntimeException("Unable to find method : " + methodName);
        }
        gOpenDexFileNative.setAccessible(true);


        // TODO: Collect the methods of custom ROM.
        try {
            gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class, String.class);
            gCameraMethodType = 1;
        } catch (NoSuchMethodException e) {
            // ignore
        }

        if (gCameraNativeSetup == null) {
            try {
                gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class, int.class, String.class);
                gCameraMethodType = 2;
            } catch (NoSuchMethodException e) {
            }
        }

        if (gCameraNativeSetup == null) {
            try {
                gCameraNativeSetup = Camera.class.getDeclaredMethod("native_setup", Object.class, int.class);
                gCameraMethodType = 3;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (gCameraNativeSetup == null) {
            Method[] methods= Camera.class.getDeclaredMethods();
            for(Method method:methods){
                if("native_setup".equals(method.getName())){
                    gCameraNativeSetup = method;
                    VLog.w("native_setup","native_setup:"+ Arrays.toString(method.getParameterTypes()));
                    break;
                }
            }
        }

        if (gCameraNativeSetup != null) {
            gCameraNativeSetup.setAccessible(true);
        }
        for (Method mth : AudioRecord.class.getDeclaredMethods()) {
            if (mth.getName().equals("native_check_permission") && mth.getParameterTypes().length == 1 && mth.getParameterTypes()[0] == String.class) {
                gAudioRecordNativeCheckPermission = mth;
                mth.setAccessible(true);
                break;
            }
        }
    }

	public static void startDexOverride() {
        List<InstalledAppInfo> installedAppInfos = VirtualCore.get().getInstalledApps(0);
        sDexOverrideMap = new HashMap<>(installedAppInfos.size());
        for (InstalledAppInfo info : installedAppInfos) {
			try {
				sDexOverrideMap.put(new File(info.apkPath).getCanonicalPath(), info);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getRedirectedPath(String origPath) {
		try {
			return nativeGetRedirectedPath(origPath);
		} catch (Throwable e) {
			VLog.e(TAG, VLog.getStackTraceString(e));
		}
        return origPath;
	}

	public static String restoreRedirectedPath(String origPath) {
		try {
			return nativeRestoreRedirectedPath(origPath);
		} catch (Throwable e) {
			VLog.e(TAG, VLog.getStackTraceString(e));
		}
        return origPath;
	}

    public static void redirectDirectory(String origPath, String newPath) {
        if (!origPath.endsWith("/")) {
            origPath = origPath + "/";
        }
        if (!newPath.endsWith("/")) {
            newPath = newPath + "/";
        }
        try {
            nativeRedirect(origPath, newPath);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }
    public static void redirectFile(String origPath, String newPath) {
        if (origPath.endsWith("/")) {
            origPath = origPath.substring(0, origPath.length() - 1);
        }
        if (newPath.endsWith("/")) {
            newPath = newPath.substring(0, newPath.length() - 1);
        }
		try {
			nativeRedirect(origPath, newPath);
		} catch (Throwable e) {
			VLog.e(TAG, VLog.getStackTraceString(e));
		}
	}

    public static void readOnly(String path) {
        try {
            nativeReadOnly(path);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }

    public static void whiteList(String path) {
        try {
            nativeWhiteList(path);
        } catch (Throwable e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
    }
	public static void hook() {
		try {
			nativeHook(Build.VERSION.SDK_INT);
		} catch (Throwable e) {
			VLog.e(TAG, VLog.getStackTraceString(e));
		}
	}

	public static void hookNative() {
        if (sFlag) {
            return;
        }
        Method[] methods = {gOpenDexFileNative, gCameraNativeSetup, gAudioRecordNativeCheckPermission};
		try {
            nativeHookNative(methods, VirtualCore.get().getHostPkg(), VirtualRuntime.isArt(), Build.VERSION.SDK_INT, gCameraMethodType);
		} catch (Throwable e) {
			VLog.e(TAG, VLog.getStackTraceString(e));
		}
		sFlag = true;
	}

	public static int onKillProcess(int pid, int signal) {
		VLog.e(TAG, "killProcess: pid = %d, signal = %d.", pid, signal);
		if (pid == android.os.Process.myPid()) {
			VLog.e(TAG, VLog.getStackTraceString(new Throwable()));
            StubService.stop(VirtualCore.get().getContext(), VClientImpl.get().getVPid());
		}
        if (VClientImpl.get().getCurrentPackage().equals("com.imo.android.imoim")) {
            if (pid != android.os.Process.myPid() ){
                return 1;
            }
        }
        return 0;
	}

    public static void notifyNativeCrash(int signal) {
        VLog.logbug(TAG, "notifyNativeCrash: signal = " + signal);
        Exception ex = new Exception("native crash: "+ signal + " in pid: " + Process.myPid());
        VLog.logbug(TAG, VLog.getStackTraceString(ex));
        //StubService.stop(VirtualCore.get().getContext(), VClientImpl.get().getVPid());
        //Thread.currentThread().stop(new Exception("native crash: "+ signal));
        //CrashReport.postCatchedException(ex);
        try {
            Intent crash = new Intent("appclone.intent.action.SHOW_CRASH_DIALOG");
            crash.putExtra("package", VClientImpl.get().getCurrentPackage());
            crash.putExtra("exception", ex);
            crash.putExtra("tag", 47414);
            VirtualCore.get().getContext().sendBroadcast(crash);
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
	}

	public static int onGetCallingUid(int originUid) {
		int callingPid = Binder.getCallingPid();
		if (callingPid == Process.myPid()) {
			return VClientImpl.get().getBaseVUid();
		}
		if (callingPid == VirtualCore.get().getSystemPid()) {
			return Process.SYSTEM_UID;
		}
		int vuid = VActivityManager.get().getUidByPid(callingPid);
		if (vuid != -1) {
            return VUserHandle.getAppId(vuid);
        }
        VLog.d(TAG, "Unknown uid: " + callingPid);
		return VClientImpl.get().getBaseVUid();
	}

	public static void onOpenDexFileNative(String[] params) {
		String dexOrJarPath = params[0];
		String outputPath = params[1];
		VLog.d(TAG, "DexOrJarPath = %s, OutputPath = %s.", dexOrJarPath, outputPath);
		try {
			String canonical = new File(dexOrJarPath).getCanonicalPath();
			VLog.d(TAG, "canonical DexOrJarPath = %s", canonical);
            InstalledAppInfo info = sDexOverrideMap.get(canonical);
			if (info != null && !info.dependSystem) {
				outputPath = info.getOdexFile().getPath();
				params[1] = outputPath;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


    private static native void nativeHookNative(Object method, String hostPackageName, boolean isArt, int apiLevel, int cameraMethodType);

	private static native void nativeMark();


	private static native String nativeRestoreRedirectedPath(String redirectedPath);

	private static native String nativeGetRedirectedPath(String orgPath);

    private static native void nativeRedirect(String origPath, String newPath);

    private static native void nativeReadOnly(String path);
    private static native void nativeWhiteList(String path);
	private static native void nativeHook(int apiLevel);

	public static int onGetUid(int uid) {
		return VClientImpl.get().getBaseVUid();
	}
}
