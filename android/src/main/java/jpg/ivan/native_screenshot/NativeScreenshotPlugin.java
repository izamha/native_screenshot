package jpg.ivan.native_screenshot;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.renderer.FlutterRenderer;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterView;

/**
 * NativeScreenshotPlugin
 */
public class NativeScreenshotPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {
	private static final String TAG = "NativeScreenshotPlugin";

	private Context context;
	private MethodChannel channel;
	private Activity activity;
	private Object renderer;

	private boolean ssError = false;
	private String ssPath;

	// Default constructor for old registrar
	public NativeScreenshotPlugin() {
	} // NativeScreenshotPlugin()

	// Condensed logic to initialize the plugin
	private void initPlugin(Context context, BinaryMessenger messenger, Activity activity, Object renderer) {
		this.context = context;
		this.activity = activity;
		this.renderer = renderer;

		this.channel = new MethodChannel(messenger, "native_screenshot");
		this.channel.setMethodCallHandler(this);
	} // initPlugin()

	// New v2 listener methods
	@Override
	public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
		this.channel.setMethodCallHandler(null);
		this.channel = null;
		this.context = null;
	} // onDetachedFromEngine()

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		Log.println(Log.INFO, TAG, "Using *NEW* registrar method!");

		initPlugin(
				flutterPluginBinding.getApplicationContext(),
				flutterPluginBinding.getBinaryMessenger(),
				null,
				flutterPluginBinding.getFlutterEngine().getRenderer()
				// flutterPluginBinding.getEngineGroup()
		); // initPlugin()
	} // onAttachedToEngine()

	// Old v1 register method
	// FIX: Make instance variables set with the old method
	public static void registerWith(Registrar registrar) {
		Log.println(Log.INFO, TAG, "Using *OLD* registrar method!");

		NativeScreenshotPlugin instance = new NativeScreenshotPlugin();

		instance.initPlugin(
				registrar.context(),
				registrar.messenger(),
				registrar.activity(),
				registrar.view()
		); // initPlugin()
	} // registerWith()


	// Activity condensed methods
	private void attachActivity(ActivityPluginBinding binding) {
		this.activity = binding.getActivity();
	} // attachActivity()

	private void detachActivity() {
		this.activity = null;
	} // attachActivity()


	// Activity listener methods
	@Override
	public void onAttachedToActivity(ActivityPluginBinding binding) {
		attachActivity(binding);
	} // onAttachedToActivity()

	@Override
	public void onDetachedFromActivityForConfigChanges() {
		detachActivity();
	} // onDetachedFromActivityForConfigChanges()

	@Override
	public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
		attachActivity(binding);
	} // onReattachedToActivityForConfigChanges()

	@Override
	public void onDetachedFromActivity() {
		detachActivity();
	} // onDetachedFromActivity()


	// MethodCall, manage stuff coming from Dart
	@Override
	public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
		if( !permissionToWrite() ) {
			Log.println(Log.INFO, TAG, "Permission to write files missing!");

			result.success(null);

			return;
		} // if cannot write

		if( !call.method.equals("takeScreenshot") ) {
			Log.println(Log.INFO, TAG, "Method not implemented!");

			result.notImplemented();

			return;
		} // if not implemented


		// Need to fix takeScreenshot()
		// it produces just a black image
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// takeScreenshot();
			takeScreenshotOld();
		} else {
			takeScreenshotOld();
		} // if

		if( this.ssError || this.ssPath == null || this.ssPath.isEmpty() ) {
			result.success(null);

			return;
		} // if error

		result.success(this.ssPath);
	} // onMethodCall()


	// Own functions, plugin specific functionality
	private String getScreenshotName() {
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
		String sDate = sf.format(new Date());
		return "askfield-" + sDate + ".png";
	}

	private String getApplicationName() {
		ApplicationInfo appInfo = null;

		try {
			appInfo = this.context.getPackageManager()
					.getApplicationInfo(this.context.getPackageName(), 0);
		} catch (Exception ex) {
			Log.println(Log.INFO, TAG, "Error getting package name, using default. Err: " + ex.getMessage());
		}

		if(appInfo == null) {
			return "NativeScreenshot";
		} // if null

		CharSequence cs = this.context.getPackageManager().getApplicationLabel(appInfo);
		StringBuilder name = new StringBuilder( cs.length() );

		name.append(cs);

		if( name.toString().trim().isEmpty() ) {
			return "NativeScreenshot";
		}

		return name.toString();
	} // getApplicationName()

	private String getScreenshotPath() {
		String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();

		String sDir = externalDir
				+ File.separator
				+ getApplicationName();

		File dir = new File(sDir);

		String dirPath;

		if( dir.exists() || dir.mkdir()) {
			dirPath = sDir + File.separator + getScreenshotName();
		} else {
			dirPath = externalDir + File.separator + getScreenshotName();
		}

		Log.println(Log.INFO, TAG, "Built ScreenshotPath: " + dirPath);

		return dirPath;
	} // getScreenshotPath()

	private String saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
		boolean saved;
		OutputStream fos;
		ContentResolver resolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
		Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
		fos = resolver.openOutputStream(imageUri);
		saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		fos.flush();
		fos.close();
		String finalPath = getRealPathFromURI(context, imageUri);
 		return finalPath;
	}

	public String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private String writeBitmap(Bitmap bitmap) {
		try {
			String path = getScreenshotPath();
			File imageFile = new File(path);
			FileOutputStream oStream = new FileOutputStream(imageFile);

			bitmap.compress(Bitmap.CompressFormat.PNG, 100, oStream);
			oStream.flush();
			oStream.close();

			return path;
		} catch (Exception ex) {
			Log.println(Log.INFO, TAG, "Error writing bitmap: " + ex.getMessage());
		}

		return null;
	} // writeBitmap()

	private void reloadMedia() {
		try {
			Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			File file = new File(this.ssPath);
			Uri uri = Uri.fromFile(file);

			intent.setData(uri);
			this.activity.sendBroadcast(intent);
		} catch (Exception ex) {
			Log.println(Log.INFO, TAG, "Error reloading media lib: " + ex.getMessage());
		}
	} // reloadMedia()

	private void takeScreenshot() {
		Log.println(Log.INFO, TAG, "Trying to take screenshot [new way]");

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			this.ssPath = null;
			this.ssError = true;

			return;
		}

		try {
			Window window = this.activity.getWindow();
			View view = this.activity.getWindow().getDecorView().getRootView();

			Bitmap bitmap = Bitmap.createBitmap(
					view.getWidth(),
					view.getHeight(),
					Bitmap.Config.ARGB_8888
			); // Bitmap()

			Canvas canvas = new Canvas(bitmap);
			view.draw(canvas);

//			int[] windowLocation = new int[2];
//			view.getLocationInWindow(windowLocation);
//
//			PixelListener listener = new PixelListener();
//
//			PixelCopy.request(
//					window,
//              new Rect(
//                      windowLocation[0],
//                      windowLocation[1],
//                      windowLocation[0] + view.getWidth(),
//                      windowLocation[1] + view.getHeight()
//              ),
//					bitmap,
//					listener,
//					new Handler()
//			); // PixelCopy.request()
//
//			if( listener.hasError() ) {
//				this.ssError = true;
//				this.ssPath = null;
//
//				return;
//			} // if error

			String path = writeBitmap(bitmap);
			if( path == null || path.isEmpty() ) {
				this.ssPath = null;
				this.ssError = true;
			} // if no path

			this.ssError = false;
			this.ssPath = path;

			reloadMedia();
		} catch (Exception ex) {
			Log.println(Log.INFO, TAG, "Error taking screenshot: " + ex.getMessage());
		}
	} // takeScreenshot()

	private void takeScreenshotOld() {
		Log.println(Log.INFO, TAG, "Trying to take screenshot [old way]");

		try {
			View view = this.activity.getWindow().getDecorView().getRootView();

			view.setDrawingCacheEnabled(true);

			Bitmap bitmap = null;
			if (this.renderer.getClass() == FlutterView.class) {
				bitmap = ((FlutterView) this.renderer).getBitmap();
			} else if(this.renderer.getClass() == FlutterRenderer.class ) {
				bitmap = ( (FlutterRenderer) this.renderer ).getBitmap();
			}

			if(bitmap == null) {
				this.ssError = true;
				this.ssPath = null;

				Log.println(Log.INFO, TAG, "The bitmap cannot be created :(");

				return;
			} // if

			view.setDrawingCacheEnabled(false);

			String path = saveImage(bitmap, getScreenshotName());
			if( path == null || path.isEmpty() ) {
				this.ssError = true;
				this.ssPath = null;

				Log.println(Log.INFO, TAG, "The bitmap cannot be written, invalid path.");

				return;
			} // if

			this.ssError = false;
			this.ssPath = path;

			reloadMedia();
		} catch (Exception ex) {
			Log.println(Log.INFO, TAG, "Error taking screenshot: " + ex.getMessage());
		}
	} // takeScreenshot()

	private boolean permissionToWrite() {
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			Log.println(Log.INFO, TAG, "Permission to write false due to version codes.");

			return false;
		}

		int perm = this.activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if(perm == PackageManager.PERMISSION_GRANTED) {
			Log.println(Log.INFO, TAG, "Permission to write granted!");

			return true;
		} // if

		Log.println(Log.INFO, TAG, "Requesting permissions...");
		this.activity.requestPermissions(
				new String[]{
						Manifest.permission.WRITE_EXTERNAL_STORAGE
				},
				11
		); // requestPermissions()

		Log.println(Log.INFO, TAG, "No permissions :(");

		return false;
	} // permissionToWrite()
} // NativeScreenshotPlugin
