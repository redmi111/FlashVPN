package com.polestar.clone.client.hook.utils;

import android.os.Build;

import com.polestar.clone.client.core.VirtualCore;
import com.polestar.clone.helper.utils.ArrayUtils;
import com.polestar.clone.os.VUserHandle;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Lody
 *
 */
public class MethodParameterUtils {

	public static <T> T getFirstParam(Object[] args, Class<T> tClass) {
		if (args == null) {
			return null;
		}
		int index = ArrayUtils.indexOfFirst(args, tClass);
		if (index != -1) {
			return (T) args[index];
		}
		return null;
	}

	public static String replaceFirstAppPkg(Object[] args) {
		if (args == null) {
			return null;
		}
		int index = ArrayUtils.indexOfFirst(args, String.class);
		if (index != -1) {
			String pkg = (String) args[index];
			args[index] = VirtualCore.get().getHostPkg();
			return pkg;
		}
		return null;
	}

	public static String replaceLastAppPkg(Object[] args) {
		int index = ArrayUtils.indexOfLast(args, String.class);
		if (index != -1) {
			String pkg = (String) args[index];
			args[index] = VirtualCore.get().getHostPkg();
			return pkg;
		}
		return null;
	}

	public static String replaceSequenceAppPkg(Object[] args, int sequence) {
		int index = ArrayUtils.indexOf(args, String.class, sequence);
		if (index != -1) {
			String pkg = (String) args[index];
			args[index] = VirtualCore.get().getHostPkg();
			return pkg;
		}
		return null;
	}

	public static Class<?>[] getAllInterface(Class clazz){
		HashSet<Class<?>> classes = new HashSet<>();
		getAllInterfaces(clazz,classes);
		Class<?>[] result=new Class[classes.size()];
		classes.toArray(result);
		return result;
	}


	public static void getAllInterfaces(Class clazz, HashSet<Class<?>> interfaceCollection) {
		Class<?>[] classes = clazz.getInterfaces();
		if (classes.length != 0) {
			interfaceCollection.addAll(Arrays.asList(classes));
		}
		if (clazz.getSuperclass() != Object.class) {
			getAllInterfaces(clazz.getSuperclass(), interfaceCollection);
		}
	}


	public static void replaceLastUserId(Object[] args) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int index = ArrayUtils.indexOfLast(args, Integer.class);
			if (index != -1) {
				int userId = (int) args[index];
				if (userId == VUserHandle.myUserId()) {
					args[index] = VUserHandle.getHostUserId();
				}
			}
		}
	}

	public static void replaceUserId(Object[] args, int index) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int userId = (int) args[index];
			if (userId == VUserHandle.myUserId()) {
				args[index] = VUserHandle.getHostUserId();
			}
		}
	}
}
