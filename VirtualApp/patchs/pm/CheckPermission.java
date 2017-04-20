package com.lody.virtual.client.hook.patchs.pm;

import android.Manifest;
import android.content.pm.PackageManager;

import com.lody.virtual.client.hook.base.Hook;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VUserHandle;

import java.lang.reflect.Method;

/**
 * @author Lody
 *
 *
 * @see android.content.pm.IPackageManager#checkPermission(String, String, int)
 */
/* package */ class CheckPermission extends Hook {

	@Override
	public String getName() {
		return "checkPermission";
	}

	@Override
	public Object call(Object who, Method method, Object... args) throws Throwable {
		String permName = (String) args[0];
		String pkgName = (String) args[1];
		int userId = VUserHandle.myUserId();
		if (permName.startsWith("com.google")) {
			return PackageManager.PERMISSION_GRANTED;
		}
		if (Manifest.permission.ACCOUNT_MANAGER.equals(permName)) {
			return PackageManager.PERMISSION_GRANTED;
		}
		if (!permName.startsWith("android.permission")) {
			return PackageManager.PERMISSION_GRANTED;
		}
		return VPackageManager.get().checkPermission(permName, pkgName, userId);
	}

	@Override
	public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
		return super.afterCall(who, method, args, result);
	}

	@Override
	public boolean isEnable() {
		return isAppProcess();
	}
}