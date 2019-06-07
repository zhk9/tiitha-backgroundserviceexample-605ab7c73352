package com.geoape.backgroundlocationexample;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;


public class BackgroundService extends Service {

    private int numRunningActivities = 0;
    private static int NOTIFICATION_ID = 123456;
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private final String TAG = "BackgroundService";

    // public static final String TAG = UStats.class.getSimpleName();
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private NotificationManager notificationManager;
    private android.os.Handler handler = new android.os.Handler();
    private Timer timer = new Timer();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-yyyy HH:mm:ss");

    private List<String> listPackageName = new ArrayList<>();
    private List<String> listAppName = new ArrayList<>();
    String current = "NULL";
    String previous = "NULL";
    String timeleft = "NULL";

    long startTime = 0;
    long previousStartTime = 0;
    long endTime = 0;
    long totlaTime = 0;


    public File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), albumName);
        //  if (file.exists()) {
        //    return file;
        // }
        if (!file.mkdirs()) {
            Toast.makeText(getApplicationContext(), "Directory not created", Toast.LENGTH_SHORT).show();
        }
        return file;
    }


    int LOCATION_INTERVAL = 20000;
    int LOCATION_DISTANCE = 0;


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class LocationListener implements android.location.LocationListener {
        private Location lastLocation = null;
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }


        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {


                long time = System.currentTimeMillis();
                String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));


                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                //  String currentApp = "NULL";


                String loc = date + "\t" + latitude + "\t" + longitude + "\t" + "\n";
                try {
                    File data = new File("GPS.txt");
                    FileOutputStream fos = openFileOutput("GPS.txt", Context.MODE_APPEND);
                    fos.write((loc).getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                mLastLocation = location;
                Log.i(TAG, "LocationChanged: " + location);

            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + status);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public class SensorRestarterBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(SensorRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
            context.startService(new Intent(context, BackgroundService.class));
            ;
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        startForeground(12345678, getNotification());
        installedapp();
        List apps = new ArrayList<>();
        final String[] activityOnTop = {null};

        PackageManager packageManager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appList = packageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(packageManager));
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            ApplicationInfo a = p.applicationInfo;
            // skip system apps if they shall not be included
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                continue;
            }
            apps.add(p.packageName);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        aggregationapp();

                    }
                });
            }
        }, 0, 1000);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        printForegroundTask();

                    }
                });
            }
        }, 0, 5000);
        getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(Activity activity) {
                numRunningActivities++;
                String appname = getApplicationName(getApplicationContext());
                //    Toast.makeText(getApplicationContext(),"the app name is :"+ appname,Toast.LENGTH_LONG).show();
                if (numRunningActivities == 1) {
                    Log.d("APPLICATION", "APP IN FOREGROUND");
                }

            }

            @Override
            public void onActivityStopped(Activity activity) {

                //   Toast.makeText(getApplicationContext(),"bg :",Toast.LENGTH_LONG).show();

                numRunningActivities--;
                if (numRunningActivities == 0) {
                    Log.e("", "App is in BACKGROUND");
                }

            }


            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }


            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }
        });

    }


    private android.location.LocationListener listener = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d("Loc", "Got new location");

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            String loc = currentDateandTime + "," + location.getLatitude() + "," + location.getLongitude();
            String path = "";
            if (isExternalStorageWritable() && isExternalStorageReadable()) {
                File file = new File(getPublicAlbumStorageDir("data"), "data.txt");
                try {
                    FileOutputStream fos = new FileOutputStream(file, true);
                    fos.write((loc + "\n").getBytes());
                    fos.close();
                } catch (Exception e) {
                    Log.d("Loc", e.toString());
                }
                path = file.getAbsolutePath();
            } else {
                path = "Failed to create file.";
            }
            Intent i = new Intent("location_update");
            i.putExtra("path", path);
            i.putExtra("location", loc);
            sendBroadcast(i);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }

        }
    }


    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener);

        } catch (java.lang.SecurityException ex) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    public void stopTracking() {
        this.onDestroy();
        // this.aggregate();
    }

    private Notification getNotification() {


        NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }


    public class LocationServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    /* private void aggregate(){
         String currentApp = "NULL";
         String currApp="NULL";
         String appName="Null";
         String apptime="NULL";
         String  lastknown="NULL";
         String firsttime="NULL";
         // String lasttamp="NULL";
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
             UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
             long time = System.currentTimeMillis();
             List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
             if (appList != null && appList.size() > 0) {
                 SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                 for (UsageStats usageStats : appList) {
                     mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                 }
                 if (mySortedMap != null && !mySortedMap.isEmpty()) {
                     //  DateFormat dateFormat= SimpleDateFormat.getDateTimeInstance();
                     String dateFormat = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                     currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                     apptime= calculateTime(Long.parseLong(String.valueOf(mySortedMap.get(mySortedMap.lastKey()).getTotalTimeInForeground())));
                     firsttime=dateFormat.format(String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getFirstTimeStamp())));
                     lastknown=dateFormat.format(String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeUsed())));
                     // lasttamp=dateFormat.format(String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeStamp())));


                     int index = listPackageName.indexOf(currentApp);
                     appName = listAppName.get(index);

                     Log.d("AppNameTest",appName);


                 }
             }
         } else {
             ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
             List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
             currentApp = tasks.get(0).processName;
             ArrayList<String>task=new ArrayList<String>(Collections.singleton(currentApp));
             currApp= String.valueOf(task.get(0).indexOf(currentApp));

         }
         String agg = currentApp + "\t"+ appName + "\t"+ firsttime+ "\t" + lastknown + "\t" + apptime + "\n";
         try {
             File data2= new File("aggregate.txt");
             FileOutputStream fos = openFileOutput("aggregate.txt", Context.MODE_APPEND);
             fos.write((agg).getBytes());
             fos.close();
         } catch (FileNotFoundException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } catch (IOException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }

     }

   */
    private void printForegroundTask() {
        String currentApp = "NULL";
        String currApp = "NULL";
        String appName = "Null";
        String apptime = "NULL";
        String curtemp = "NULL";
        String lastknown = "NULL";
        String firsttime = "NULL";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {

                    Log.d("out3", "inside the 3rd if loop");
                    //  DateFormat dateFormat= SimpleDateFormat.getDateTimeInstance();
                    String dateFormat = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                    curtemp = currentApp;
                    apptime = calculateTime(Long.parseLong(String.valueOf(mySortedMap.get(mySortedMap.lastKey()).getTotalTimeInForeground())));
                    firsttime = String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getFirstTimeStamp()));
                    lastknown = String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeUsed()));
                    // lasttamp=dateFormat.format(String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeStamp())));
                    int index = listPackageName.indexOf(currentApp);
                    appName = listAppName.get(index);

                    Log.d("AppNameTest", appName);


                }
            }
        } else {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
            ArrayList<String> task = new ArrayList<String>(Collections.singleton(currentApp));
            //  currApp= String.valueOf(task.get(0).indexOf(currentApp));

        }


        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode()) {
            Log.d("LOCKED", "pHONE IS LOCKED");

        } else {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            Log.e("APPLICATION8", "Current App in foreground is: " + currentApp);
            String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
            String curr = date + "\t" + longitude + "\t" + latitude + "\t" + currentApp + "\t" + appName + "\n";
            if (!currentApp.equals("com.google.android.googlequicksearchbox") && (!currentApp.equals("com.google.android.apps.nexuslauncher")) && (!currentApp.equals("android"))) {
                try {
                    File data2 = new File("opened.txt");
                    FileOutputStream fos = openFileOutput("opened.txt", Context.MODE_APPEND);
                    fos.write((curr).getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            // String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
            String timerapp = currentApp + "\t" + appName + "\t" + firsttime + "\t" + lastknown + "\t" + apptime + "\n";
            if (!currentApp.equals("com.google.android.googlequicksearchbox") && (!currentApp.equals("com.google.android.apps.nexuslauncher")) && (!currentApp.equals("android"))) {
                try {
                    File data2 = new File("duration.txt");
                    FileOutputStream fos = openFileOutput("duration.txt", Context.MODE_APPEND);
                    fos.write((timerapp).getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.d("LOCKED", "pHONE IS UNLOCKED");
                //it is not locked
            }
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        Log.e("APPLICATION8", "Current App in foreground is: " + currentApp);

        String date = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
        String curr = date + "\t" + longitude + "\t" + latitude + "\t" + currentApp + "\t" + appName + "\n";
        if (!currentApp.equals("com.google.android.googlequicksearchbox") && (!currentApp.equals("android"))) {
            try {
                File data2 = new File("detailInfo.txt");
                FileOutputStream fos = openFileOutput("detailInfo.txt", Context.MODE_APPEND);
                fos.write((curr).getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Log.e("CURRENT APP", "Current App in foreground is: " + currentApp);
        }
    }

    public void installedapp() {
        List<PackageInfo> packageList = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);

            String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            String pacName = packageInfo.packageName;

            listAppName.add(appName);
            listPackageName.add(pacName);


            Log.e("APPNAME", "app is " + appName + "----" + pacName + "\n");

            String app = appName + "\t" + pacName + "\t" + "\n";
            try {
                File data3 = new File("appname.txt");
                FileOutputStream fos = openFileOutput("appname.txt", Context.MODE_APPEND);
                fos.write((app).getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private String calculateTime(long ms) {
        String total = "";
        long sec = ms / 1000;
        long day;
        long hour;
        long min;
        if (sec >= (86400)) {
            day = sec / 86400;
            sec = sec % 86400;
            total = total + day + "d";
        }
        if (sec >= 3600) {
            hour = sec / 3600;
            sec = sec % 3600;
            total = total + hour + "h";
        }
        if (sec >= 60) {
            min = sec / 60;
            sec = sec % 60;
            total = total + min + "m";
        }
        if (sec > 0) {
            total = total + sec + "s";
        }
        return total;
    }

    public void aggregationapp() {
        String lastknown = "NULL";
        String appName="NULL";
        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        Date systemDate = Calendar.getInstance().getTime();
        String myDate = sdf.format(systemDate);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (mySortedMap != null && !mySortedMap.isEmpty()) {
                String dateFormat = String.valueOf(android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", new java.util.Date()));
                current = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                java.text.DateFormat df = new java.text.SimpleDateFormat("hh:mm:ss");
                {
                    if (!current.equals(previous)) {
                        Log.d("panda", "zebra" + previous);
                        Log.d("side", "dish" + current);
                        Log.d("tims", "Horton" + myDate);

                        startTime = System.currentTimeMillis();

                        if (startTime != previousStartTime) {
                            totlaTime = startTime - previousStartTime;

                        }

                        Log.d("AppInfo","app name "+previous+ " App time" + totlaTime);
                        String appt = previous + "\t" +totlaTime + "\n";
                        try {
                            File data7= new File("individual.txt");
                            FileOutputStream fos = openFileOutput("individual.txt", Context.MODE_APPEND);
                            fos.write((appt).getBytes());
                            fos.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        previousStartTime = startTime;
                    } else if(current.equals(previous)){


                        //endTime = startTime;

                        lastknown = String.valueOf(new Date(mySortedMap.get(mySortedMap.lastKey()).getLastTimeUsed()));
                        Log.d("Birds","crow" + lastknown);
                    }
                    previous = current;

                    Log.d("zoo", "animals" + previous);


                }


            } else {
                ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            }
        }
    }
}
