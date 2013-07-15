package ir.noghteh.imagedownloaderlibrary;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Internet {

	
	public static boolean isConnect(Context context){
		ConnectivityManager cm=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfo=cm.getActiveNetworkInfo();
		
		if (activeNetInfo==null)
			return false;
		
		return activeNetInfo.isConnectedOrConnecting();
	}
}
