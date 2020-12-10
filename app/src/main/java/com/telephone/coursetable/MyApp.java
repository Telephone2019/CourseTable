package com.telephone.coursetable;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Proxy;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.telephone.coursetable.Database.AppDatabase;
import com.telephone.coursetable.Database.AppDatabaseComments;
import com.telephone.coursetable.Database.AppDatabaseCompare;
import com.telephone.coursetable.Database.AppDatabaseCompareTest;
import com.telephone.coursetable.Database.AppTestDatabase;
import com.telephone.coursetable.Database.Version;
import com.telephone.coursetable.Gson.Adapters.NoNullStringAdapter;
import com.telephone.coursetable.Http.Get;
import com.telephone.coursetable.Http.HttpConnectionAndCode;
import com.telephone.coursetable.Https.Post;
import com.telephone.coursetable.LogMe.LogMe;
import com.telephone.coursetable.proxy.MyProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static androidx.core.app.NotificationCompat.VISIBILITY_SECRET;

public class MyApp extends Application {
    final public static String PACKAGE_NAME = "com.telephone.coursetable";

    private static MyApp app;
    private static AppDatabase db;
    private static AppTestDatabase db_test;
    private static AppDatabaseCompare db_compare;
    private static AppDatabaseCompareTest db_compare_test;
    private static AppDatabaseComments db_comments;
    private static SharedPreferences sp;
    private static SharedPreferences sp_test;
    private static SharedPreferences.Editor editor;
    private static SharedPreferences.Editor editor_test;
    private static ApplicationInfo app_info;

    public static Gson gson;

    public volatile String new_version = "";

    private volatile static ArrayList<String> data_list = null;

    private volatile static MainActivity running_main = null;
    private volatile static boolean running_login_thread = false;
    private volatile static boolean running_fetch_service = false;

    public enum RunningActivity {
        MAIN, LOGIN, LOGIN_VPN, FUNCTION_MENU, CHANGE_HOURS, CHANGE_TERMS, LIBRARY, ABOUT, USAGE,
        WEB_LINKS, GUET_MUSIC, GUET_PHONE, WEB_INFO, IMAGE_MAP, GRADE_POINTS, TEST, COURSE_CARD,
        EDIT_COURSE, TEACHER_EVALUATION_PANEL, COMMENT, NULL
    }

    private volatile static RunningActivity running_activity = RunningActivity.NULL;
    private volatile static AppCompatActivity running_activity_pointer = null;

    public static synchronized void clearRunningActivity(AppCompatActivity ac) {
        final String NAME = "clearRunningActivity()";
        if (ac != null) {
            com.telephone.coursetable.LogMe.LogMe.e(NAME, "on destroy activity pointer = " + ac.toString());
        }
        if (running_activity_pointer != null) {
            com.telephone.coursetable.LogMe.LogMe.e(NAME, "cached running activity: " + running_activity + " pointer = " + running_activity_pointer.toString());
        }
        if (ac != null && running_activity_pointer != null) {
            if (ac.toString().equals(running_activity_pointer.toString())) {
                running_activity = RunningActivity.NULL;
                com.telephone.coursetable.LogMe.LogMe.e(NAME, "remove cached running activity pointer = " + running_activity_pointer.toString());
                running_activity_pointer = null;
            }
        }
    }

    public static synchronized AppCompatActivity getRunning_activity_pointer() {
        return running_activity_pointer;
    }

    public static synchronized void setRunning_activity_pointer(AppCompatActivity running_activity_pointer) {
        MyApp.running_activity_pointer = running_activity_pointer;
    }

    public static synchronized ArrayList<String> getData_list() {
        return data_list;
    }

    public static synchronized void setData_list(ArrayList<String> data_list) {
        MyApp.data_list = data_list;
    }

    public static synchronized MainActivity getRunning_main() {
        return running_main;
    }

    public static synchronized void setRunning_main(MainActivity running_main) {
        MyApp.running_main = running_main;
    }

    public static synchronized boolean isRunning_login_thread() {
        return running_login_thread;
    }

    public static synchronized void setRunning_login_thread(boolean running_login_thread) {
        MyApp.running_login_thread = running_login_thread;
    }

    public static synchronized boolean isRunning_fetch_service() {
        return running_fetch_service;
    }

    public static synchronized void setRunning_fetch_service(boolean running_fetch_service) {
        MyApp.running_fetch_service = running_fetch_service;
    }

    public static synchronized RunningActivity getRunning_activity() {
        return running_activity;
    }

    public static synchronized void setRunning_activity(RunningActivity running_activity) {
        MyApp.running_activity = running_activity;
        LogMe.init(); // cancel all takeover of log system from any activity
    }

    final public static String ocr_lang_code = "telephone";
    final public static String notification_channel_id_running = "running";
    final public static String notification_channel_id_update = "update";
    final public static String notification_channel_id_fetch_fail = "fetch_fail";
    final public static String notification_channel_id_normal = "normal";
    final public static String notification_channel_id_new_data = "new_data";
    final public static String notification_channel_name_running = "前台服务通知";
    final public static String notification_channel_name_update = "应用更新通知";
    final public static String notification_channel_name_fetch_fail = "自动同步异常通知";
    final public static String notification_channel_name_normal = "常规通知";
    final public static String notification_channel_name_new_data = "自动同步新数据通知";
    final public static String notification_channel_des_running = "展示APP正在运行的通知";
    final public static String notification_channel_des_update = "提醒APP有新版本发布的通知";
    final public static String notification_channel_des_fetch_fail = "提醒自动同步出现异常的通知";
    final public static String notification_channel_des_normal = "普通通知";
    final public static String notification_channel_des_new_data = "提醒自动同步拉取到了新数据的通知";
    final public static int notification_id_fetch_service_foreground = 1800301129;
    final public static int notification_id_fetch_service_lan_password_wrong = 1800301127;
    final public static int notification_id_new_version = 1800301128;
    final public static int notification_id_click_to_login = 1800301130;
    final public static int notification_id_new_grade = 1;
    final public static int notification_id_new_exam = 2;
    final public static long service_fetch_interval = 15000;   // 15s
    final public static long patient_time = 4000;   //4s
    final public static boolean ip_override = true;
    final public static String guet_v_ip = "202.193.64.75";
    final public static String guet_v_domain = "v.guet.edu.cn";
    final public static int check_code_regain_times = 6;
    final public static int web_vpn_relogin_times = 2;
    final public static int web_vpn_wck_times = 6;
    final public static int web_vpn_ticket_regain_times = 6;
    final public static int web_vpn_refetch_times = 2;
    final public static int lan_fetch_normal_read_timeout = 30_000; // 10s --> 30s
    final public static int lan_fetch_lab_read_timeout = 120_000; // 30s --> 120s
    final public static int wan_fetch_lab_read_timeout = 30_000; // 30s
    final public static String[] appwidget_list_today_time_descriptions = {
            "今天: 第一大节 (上午)",
            "今天: 第二大节 (上午)",
            "今天: 第三大节 (下午)",
            "今天: 第四大节 (下午)",
            "今天: 第五大节 (晚上)"
    };
    final public static String[] appwidget_list_tomorrow_time_descriptions = {
            "明天: 第一大节 (上午)",
            "明天: 第二大节 (上午)",
            "明天: 第三大节 (下午)",
            "明天: 第四大节 (下午)",
            "明天: 第五大节 (晚上)"
    };
    final public static String[] times = {"1", "2", "3", "4", "5"};
    final public static int[] timetvIds = {
            R.id.textView_time1, //times[0]
            R.id.textView_time2, //times[1]
            R.id.textView_time3, //times[2]
            R.id.textView_time4, //times[3]
            R.id.textView_time5 //times[4]
    };
    final public static int[] weekdaytvIds = {
            R.id.textView_wd1,
            R.id.textView_wd2,
            R.id.textView_wd3,
            R.id.textView_wd4,
            R.id.textView_wd5,
            R.id.textView_wd6,
            R.id.textView_wd7
    };
    final public static int[][] nodeIds = {
            {R.id.textView1, R.id.textView2, R.id.textView3, R.id.textView4, R.id.textView5, R.id.textView6, R.id.textView7},//times[0]
            {R.id.textView8, R.id.textView9, R.id.textView10, R.id.textView11, R.id.textView12, R.id.textView13, R.id.textView14},//times[1]
            {R.id.textView15, R.id.textView16, R.id.textView17, R.id.textView18, R.id.textView19, R.id.textView20, R.id.textView21},//times[2]
            {R.id.textView22, R.id.textView23, R.id.textView24, R.id.textView25, R.id.textView26, R.id.textView27, R.id.textView28},//times[3]
            {R.id.textView29, R.id.textView30, R.id.textView31, R.id.textView32, R.id.textView33, R.id.textView34, R.id.textView35}//times[4]
    };
    final public static int[] restLineIds = {
            R.id.main_rest_01,
            R.id.main_rest_12,
            R.id.main_rest_23,
            R.id.main_rest_34,
            R.id.main_rest_45
    };

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        db = Room.databaseBuilder(this, AppDatabase.class, "telephone-db").enableMultiInstanceInvalidation().fallbackToDestructiveMigration().build();
        db_compare = Room.databaseBuilder(this, AppDatabaseCompare.class, "telephone-db-compare").enableMultiInstanceInvalidation().fallbackToDestructiveMigration().build();
        db_test = Room.databaseBuilder(this, AppTestDatabase.class, "telephone-db-test").enableMultiInstanceInvalidation().fallbackToDestructiveMigration().build();
        db_compare_test = Room.databaseBuilder(this, AppDatabaseCompareTest.class, "telephone-db-compare-test").enableMultiInstanceInvalidation().fallbackToDestructiveMigration().build();
        db_comments = Room.databaseBuilder(this, AppDatabaseComments.class, "telephone-db-comments").enableMultiInstanceInvalidation().fallbackToDestructiveMigration().build();
        sp = getSharedPreferences(getResources().getString(R.string.preference_file_name), MODE_PRIVATE);
        sp_test = getSharedPreferences(getResources().getString(R.string.preference_file_name_test), MODE_PRIVATE);
        editor = sp.edit();
        editor_test = sp_test.edit();
        app_info = getApplicationInfo();

        // make Gson serialize null String fields to "", other "null"
        gson = new GsonBuilder().registerTypeAdapter(String.class, new NoNullStringAdapter()).serializeNulls().create();
        LogMe.init();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel;
            channel = new NotificationChannel(notification_channel_id_running, notification_channel_name_running, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(notification_channel_des_running);
            channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            channel = new NotificationChannel(notification_channel_id_update, notification_channel_name_update, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(notification_channel_des_update);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            channel = new NotificationChannel(notification_channel_id_fetch_fail, notification_channel_name_fetch_fail, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(notification_channel_des_fetch_fail);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            channel = new NotificationChannel(notification_channel_id_normal, notification_channel_name_normal, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(notification_channel_des_normal);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            channel = new NotificationChannel(notification_channel_id_new_data, notification_channel_name_new_data, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(notification_channel_des_new_data);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        FetchService.startAction_START_FETCH_DATA(this, service_fetch_interval, null);

        new Thread(() -> {
            if (getCurrentAppDB().versionDao().selectVersion(BuildConfig.VERSION_NAME).isEmpty()) {
                HttpConnectionAndCode report_res = Post.post(
                        "https://guetcob.com:44334/reportversion",
                        null,
                        "",
                        "",
                        BuildConfig.VERSION_NAME,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true
                );
                if (report_res.code == 0) {
                    getCurrentAppDB().versionDao().insert(new Version(BuildConfig.VERSION_NAME));
                }
            }
        }).start();
    }

    public static MyApp getCurrentApp() {
        return app;
    }

    public static AppDatabase getCurrentAppDB() {
        return db;
    }

    public static AppTestDatabase getCurrentAppDB_Test() {
        return db_test;
    }

    public static AppDatabaseCompare getDb_compare() {
        return db_compare;
    }

    public static AppDatabaseCompareTest getDb_compare_test() {
        return db_compare_test;
    }

    public static AppDatabaseComments getDb_comments() {
        return db_comments;
    }

    public static SharedPreferences getCurrentSharedPreference() {
        return sp;
    }

    public static SharedPreferences getCurrentSharedPreference_Test() {
        return sp_test;
    }

    public static SharedPreferences.Editor getCurrentSharedPreferenceEditor() {
        return editor;
    }

    public static SharedPreferences.Editor getCurrentSharedPreferenceEditor_Test() {
        return editor_test;
    }

    public static ApplicationInfo getApplicationInfo_me() {
        return app_info;
    }

    public static boolean isDebug() {
        return ((MyApp.getApplicationInfo_me().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

    public static boolean isLAN() {
        setProxy(false);
        HttpConnectionAndCode res = Get.get(
                "http://172.16.13.22/",
                null,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.89 Safari/537.36",
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                500,
                500
        );
        boolean b = res.resp_code == 200;
        if (!b) setProxy(true);
        return b;
    }

    public static void batteryOptimization(AppCompatActivity c) {
        String pac_name = "com.telephone.coursetable";
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(pac_name)) {
            c.startActivity(new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + pac_name)));
        } else {
            c.runOnUiThread(() -> Toast.makeText(c, "忽略电池优化已设置成功", Toast.LENGTH_LONG).show());
        }
    }

    private static Map<Integer, MyProxy> proxyMap = new HashMap<>();

    public static Map<Integer, MyProxy> getProxyMap() {
        if (proxyMap == null)
            proxyMap = new HashMap<>();
        return proxyMap;
    }

    private static boolean proxy = false;

    public static boolean isProxy() {
        return proxy;
    }

    public static void setProxy(boolean proxy) {
        MyApp.proxy = proxy;
    }
}

