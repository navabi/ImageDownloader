# ImageDownlader for Android

This project provide an instrument for downlaoding,caching and displaying images. 

Most important feature of this library is downoading images with progress by percentages that make you able to monitor and handle downloaded precentages of image using a callback

This library is base on [Ion](https://github.com/koush/ion)

![Screenshot](https://raw.github.com/navabi/ImageDownloader/master/raw/imagedownloader.png)

Android 2.1+ support

## Quick Setup

#### 1. Include library

**Manual:**
 * [Download JAR](https://github.com/navabi/ImageDownloader/raw/master/raw/imagedownloaderlibrary.jar)
 * Put the JAR in the **libs** subfolder of your Android project

#### 2. Example
``` java
public class MainActivity extends Activity {

	private ImageView mImg;
	private TextView mTxt;
	
	private ImageDownloader imageDownloader;
	private Toast toast;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mImg=(ImageView) findViewById(R.id.activity_main_img);
		mTxt=(TextView) findViewById(R.id.activity_main_tv);
		
		toast=Toast.makeText(this, "", Toast.LENGTH_SHORT);
		
		imageDownloader=new ImageDownloader(this);
		imageDownloader.displayImage("http://www.noghteh.ir/images/logo.png", mImg, progressCallback, downloadStatusCallBack);
	
	

	}
	
	ProgressCallback progressCallback=new ProgressCallback() {
		
		@Override
		public void onProgress(final int downloaded, final int total) {

			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mTxt.setText("downlaoded: "+downloaded+" of"+ total);
					
				}
			});
			
			
		}
	};
	
	DownloadStatusCallBack downloadStatusCallBack=new DownloadStatusCallBack() {
		
		@Override
		public void onStart() {
			toast.setText("Start Downloading!");
			toast.show();
			
		}
		
		@Override
		public void onFinish() {
			toast.setText("Finish Downloading!");
			toast.show();
			
		}
	};
	

}
```


<<<<<<< HEAD
#### 3.License

please inform us if you use this library ( navabi70-at-gmail)
=======
Copyright 2013 Mohsen Navabi

