package ir.noghteh.imagedownloaderlibrary;

import android.app.Activity;

public class IDLog {

	private static boolean DEBUG_MODE=false;
	
	public static void i(Activity activity,String msg){
		if (isDEBUG_MODE())
		android.util.Log.i(activity.getClass().getSimpleName(), msg);
	}

	public static boolean isDEBUG_MODE() {
		return DEBUG_MODE;
	}

	public static void setDEBUG_MODE(boolean dEBUG_MODE) {
		DEBUG_MODE = dEBUG_MODE;
	}
	
	
}
