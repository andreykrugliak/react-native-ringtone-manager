package com.reactlibrary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.os.Build;
import android.provider.Settings;
import android.content.Intent;
import android.os.Environment;
import android.database.Cursor;
import android.content.Context;
import android.widget.Toast;
import android.provider.ContactsContract;


import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RNRingtoneManagerModule extends ReactContextBaseJavaModule {

    private final ReactContext reactContext;
    private static final String TYPE_ALARM_KEY = "TYPE_ALARM";
    private static final String TYPE_ALL_KEY = "TYPE_ALL";
    private static final String TYPE_NOTIFICATION_KEY = "TYPE_NOTIFICATION";
    private static final String TYPE_RINGTONE_KEY = "TYPE_RINGTONE";

    final static class SettingsKeys {
        public static final String URI = "uri";
        public static final String TITLE = "title";
        public static final String ARTIST = "artist";
        public static final String SIZE = "size";
        public static final String MIME_TYPE = "mimeType";
        public static final String DURATION = "duration";
        public static final String RINGTONE_TYPE = "ringtoneType";
        public static final String CONTACT_RECORD_ID = "contactRecordId";
    }

    public RNRingtoneManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RingtoneManager";
    }

    @ReactMethod
    public void getRingtones() {
        getRingtones(RingtoneManager.TYPE_ALL);
    }

    @ReactMethod
    public void getRingtones(int ringtoneType) {

    }

    @ReactMethod
    public void play() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(reactContext, notification);
        r.play();
    }

    @ReactMethod
    public void createRingtone(ReadableMap settings) {
      String uriStr = settings.getString(SettingsKeys.URI);
      File dir = Environment.getExternalStorageDirectory();
      // File ringtone = new File(Environment.getDataDirectory() + "/user/0/com.imax.ringtone/files", uriStr);
      File ringtone = new File(reactContext.getFilesDir(), uriStr);
      ContentValues values = new ContentValues();
      // values.put(MediaStore.MediaColumns.DATA, ringtone.getAbsolutePath());
      values.put(MediaStore.MediaColumns.TITLE, settings.getString(SettingsKeys.TITLE));
      values.put(MediaStore.MediaColumns.SIZE, settings.getInt(SettingsKeys.SIZE));
      values.put(MediaStore.MediaColumns.MIME_TYPE, settings.getString(SettingsKeys.MIME_TYPE));
      values.put(MediaStore.Audio.Media.ARTIST, settings.getString(SettingsKeys.ARTIST));
      values.put(MediaStore.Audio.Media.DURATION, settings.getInt(SettingsKeys.DURATION));
      int ringtoneType = settings.getInt(SettingsKeys.RINGTONE_TYPE);
      values.put(MediaStore.Audio.Media.IS_RINGTONE, isRingtoneType(ringtoneType, RingtoneManager.TYPE_RINGTONE));
      values.put(MediaStore.Audio.Media.IS_NOTIFICATION, isRingtoneType(ringtoneType, RingtoneManager.TYPE_NOTIFICATION));
      values.put(MediaStore.Audio.Media.IS_ALARM, isRingtoneType(ringtoneType, RingtoneManager.TYPE_ALARM));
      values.put(MediaStore.Audio.Media.IS_MUSIC, false);
      // Log.d("||||||||||||||||||||||||||||| ===================================== ringtone.exists()--> ", "" + ringtone.exists());
      boolean settingsCanWrite = Settings.System.canWrite(reactContext);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (settingsCanWrite) {
          if (ringtone.exists() && reactContext.getCurrentActivity() != null) {
            //CHECK IF IT IS COPIED, IF YES, DIRECTLY SEND THE URI OF THAT FILE, ELSE COPY THE FILE
            File testRingtoneExistOrNot = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), uriStr);
            File newRingtone;
            if (testRingtoneExistOrNot.exists()) {
              //RINGTONE WAS ALREADY COPIED SO USE THAT
              newRingtone = testRingtoneExistOrNot;
            } else {
              //COPY THE RINGTONE
              newRingtone = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), uriStr);
            }
            //COPY THE FILE FROM DATA DIRECTORY TO RINGTONES DIRECTORY AND THEN USE THAT URI
            //COPY THE RINGTONE
            values.put(MediaStore.MediaColumns.DATA, newRingtone.getAbsolutePath());
            if (ringtoneType == 5) {
              // Log.d("||||||||||||||||||||||||||||| ===================================== ringtoneType IF--> ", "" + ringtoneType);
              //CONTACT RINGTONE
              ContentResolver contentResolver = getCurrentActivity().getContentResolver();
              Uri uri = MediaStore.Audio.Media.getContentUriForPath(newRingtone.getAbsolutePath());
              Uri newUri = reactContext.getContentResolver().insert(uri, values);
              String finalRingtonePath = "";
              //RINGTONE CREATION SUCCESSFULL
              Uri contactData = ContactsContract.Contacts.CONTENT_URI;
              String contactId = settings.getString(SettingsKeys.CONTACT_RECORD_ID);
              Uri localUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId);
              ContentValues localContentValues = new ContentValues();
              Uri uriOfRt = Uri.fromFile(new File(newRingtone.getAbsolutePath()));
              localContentValues.put(ContactsContract.Data.RAW_CONTACT_ID, contactId);
              localContentValues.put(ContactsContract.Data.CUSTOM_RINGTONE, uriOfRt.toString());
              contentResolver.update(localUri, localContentValues, null, null);                
              Toast.makeText(this.reactContext, new StringBuilder().append("Ringtone set successfully"), Toast.LENGTH_LONG).show();
            } else {
              //OTHER RINGTONES
              ContentResolver contentResolver = getCurrentActivity().getContentResolver();
              Uri uri = MediaStore.Audio.Media.getContentUriForPath(newRingtone.getAbsolutePath());
              Uri newUri = reactContext.getContentResolver().insert(uri, values);
              String finalRingtonePath = "";
              if (newUri != null) {
                //RINGTONE CREATION SUCCESSFULL
                RingtoneManager.setActualDefaultRingtoneUri(reactContext, ringtoneType, newUri);
                Toast.makeText(this.reactContext, new StringBuilder().append("Ringtone set successfully"), Toast.LENGTH_LONG).show();
              } else {
                //RINGTONE CREATION FAILED SO ALREADY THE RINGTONE PRESENT
                finalRingtonePath = getVideoContentUriFromFilePath(reactContext, newRingtone.getAbsolutePath());
                if (finalRingtonePath != null && finalRingtonePath != "") {
                  RingtoneManager.setActualDefaultRingtoneUri(reactContext, ringtoneType, Uri.parse(finalRingtonePath));
                  Toast.makeText(this.reactContext, new StringBuilder().append("Ringtone set successfully"), Toast.LENGTH_LONG).show();
                } else {
                  Toast.makeText(this.reactContext, new StringBuilder().append("Sorry, the ringtone couldnt be set!"), Toast.LENGTH_LONG).show();
                }
              }
            }
          } else {
            Toast.makeText(this.reactContext, new StringBuilder().append("Sorry, the ringtone couldnt be set!"), Toast.LENGTH_LONG).show();
          }
        } else {
          Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
          intent.setData(Uri.parse("package:" + this.reactContext.getCurrentActivity().getPackageName()));
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          this.reactContext.startActivity(intent);
        }
      }
    }

    @ReactMethod
    public void setRingtone(String uri) {

    }

    @ReactMethod
    public void pickRingtone() {

    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(TYPE_ALARM_KEY, RingtoneManager.TYPE_ALARM);
        constants.put(TYPE_ALL_KEY, RingtoneManager.TYPE_ALL);
        constants.put(TYPE_NOTIFICATION_KEY, RingtoneManager.TYPE_NOTIFICATION);
        constants.put(TYPE_RINGTONE_KEY, RingtoneManager.TYPE_RINGTONE);
        return constants;
    }

    /**
     * Returns true when the given ringtone type matches the ringtone to compare.
     * Will default to true if the given ringtone type is RingtoneManager.TYPE_ALL.
     * @param ringtoneType ringtone type given
     * @param ringtoneTypeToCompare ringtone type to compare to
     * @return true if the type matches or is TYPE_ALL
     */
    private boolean isRingtoneType(int ringtoneType, int ringtoneTypeToCompare) {
        return ringtoneTypeToCompare == ringtoneType || RingtoneManager.TYPE_ALL == ringtoneType;
    }

    public static String getVideoContentUriFromFilePath(Context ctx, String filePath) {

      ContentResolver contentResolver = ctx.getContentResolver();
      String ringtoneUriStr = null;
        long ringtoneId = -1;

        // This returns us content://media/external/ringtones/media (or something like that)
        // I pass in "external" because that's the MediaStore's name for the external
        // storage on my device (the other possibility is "internal")
        Uri ringtonesUri = MediaStore.Audio.Media.getContentUri("external");

        String[] projection = {MediaStore.Audio.AudioColumns._ID};

        // TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = contentResolver.query(ringtonesUri, projection, MediaStore.Audio.AudioColumns.DATA + " LIKE ?", new String[] { filePath }, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        ringtoneId = cursor.getLong(columnIndex);

        cursor.close();
        if (ringtoneId != -1 ) ringtoneUriStr = ringtonesUri.toString() + "/" + ringtoneId;
        return ringtoneUriStr;
    } 

    public void copyFile(String inputPath, String inputFile, String outputPath) {
    Log.d("||||||||||||||||||||||||||||| ===================================== inputPath--> ", "" + inputPath);
    Log.d("||||||||||||||||||||||||||||| ===================================== inputFile--> ", "" + inputFile);
    Log.d("||||||||||||||||||||||||||||| ===================================== outputPath--> ", "" + outputPath);
    InputStream in = null;
    OutputStream out = null;
    try {

        //create output directory if it doesn't exist
        File dir = new File (outputPath); 
        if (!dir.exists())
        {
            dir.mkdirs();
        }


        in = new FileInputStream(inputPath + inputFile);        
        out = new FileOutputStream(outputPath + inputFile);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        in = null;

        // write the output file (You have now copied the file)
        out.flush();
        out.close();
        out = null;        

    }  catch (FileNotFoundException fnfe1) {
        Log.e("============ tag", fnfe1.getMessage());
    }
            catch (Exception e) {
        Log.e("============ tag", e.getMessage());
    }

}
   
}
