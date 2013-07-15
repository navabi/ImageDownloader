package ir.noghteh.imagedownloaderlibrary;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.animation.Animation;
import android.widget.ImageView;

public class ImageDownloader {

	private Context mContext;
	
	private Animation animLoading=null;
	
	private IonBitmapCache ioBitmapCache;
	
	public ImageDownloader(Context context){
		mContext=context;
		
		ioBitmapCache=new IonBitmapCache(Ion.getDefault(mContext));
	}
	
	public void setLoadingAnimation(Animation animation){
		animLoading=animation;
	}
	
	public void displayImage(final String url,final ImageView imageView){
		displayImage(url, imageView, null,null);
	}
	public void displayImage(final String url,final ImageView imageView,DownloadStatusCallBack downloadCompleteCallBack){
		displayImage(url, imageView, null,downloadCompleteCallBack);
	}
	
	public void displayImage(final String url,final ImageView imageView,ProgressCallback progressCallBack,final DownloadStatusCallBack downloadCompletedCallBack) {
			
		if (!Internet.isConnect(mContext)){
			try
			{
				IDLog.i(((Activity)mContext), "load from permanent cache");
				displayCachedImageIfAvailable(url,imageView);
			}
			catch (Exception e) {}
			return;
		}
		
		try {
			BitmapInfo info=ioBitmapCache.get(CacheManager.SHA1(url));
			if (info!=null)
			{
				IDLog.i(((Activity)mContext), "load from temprory cache");
				imageView.setImageBitmap(info.bitmap);
				return;
			}
		
		} catch (Exception e){}
		
		if (downloadCompletedCallBack!=null)
			downloadCompletedCallBack.onStart();
		
		downloadImage(url, progressCallBack, new FutureCallback<ByteArrayOutputStream>() {
			
			@Override
			public void onCompleted(Exception arg0, ByteArrayOutputStream arg1) {
				
				if (arg0!=null){
					IDLog.i((Activity) mContext, "OnComplete Exeption: "+arg0);
					 return;
				}
				cachDownlaodedImage(arg1,url);
		
				displayImage(imageView, arg1.toByteArray());
				
				cachBitmapTemprory(arg1.toByteArray(),url);
				
				IDLog.i(((Activity)mContext), "load from downloda");
				
				if (downloadCompletedCallBack!=null)
					downloadCompletedCallBack.onFinish();
			}
		});
	
		
	}
	
	protected void cachBitmapTemprory(byte[] byteArray, String url) {
	
		try {
			BitmapInfo info=new BitmapInfo();
			info.key=CacheManager.SHA1(url);
			info.bitmap=BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			ioBitmapCache.put(info);
		
		} catch (Exception e){}
	
	}

	private void displayImage(ImageView imageView, byte[] bytes) {
		Bitmap bitmap=BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		imageView.setImageBitmap(bitmap);
		if (animLoading!=null)
			imageView.startAnimation(animLoading);
	}
	
	protected void cachDownlaodedImage(ByteArrayOutputStream arg1, String url) {
		try {
			CacheManager.cacheData(mContext, arg1.toByteArray(), CacheManager.SHA1(url));
		} catch (Exception e){
			
		}
	}

	private void displayCachedImageIfAvailable(String url, ImageView imageView) throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
		
		byte[] bytes=CacheManager.retrieveData(mContext, CacheManager.SHA1(url));
		
		if (bytes!=null){
			displayImage(imageView,bytes);
		}
	}
	
	private void downloadImage(String url,ProgressCallback progressCallBack,FutureCallback<ByteArrayOutputStream> futureCallBack){
		
		Ion.with(mContext,url)
		.progress(progressCallBack)
		.write(new ByteArrayOutputStream())
		.setCallback(futureCallBack)
		;
				
	}
}
