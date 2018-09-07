package org.iseclab.drammer;

public class Constants {

    public static int DEFAULT_EXIT_CODE = -1;

    public static int PERMISSION_REQUEST_IMEI = 1;

    public static String BITFLIP = "FLIP";

    public interface ARCH {
        public static String ARM    = "armeabi";
        public static String ARM64  = "arm64";
        public static String x86    = "x86";
        public static String x86_64 = "x86_64";
        public static String UNKNOWN = "unknown";
    }

    public interface ACTION {
        public static String START_HAMMERING = BuildConfig.APPLICATION_ID + "action.start_hammering";
        public static String STOP_HAMMERING = BuildConfig.APPLICATION_ID + "action.stop_hammering";
        public static String RESET_HAMMERING = BuildConfig.APPLICATION_ID + "action.reset_hammering";

        public static String ACTIVITY_PAUSE = BuildConfig.APPLICATION_ID + "action.activity_pause";
        public static String ACTIVITY_RESUME = BuildConfig.APPLICATION_ID + "action.activity_resume";
    }

    public interface BROADCAST {
        public static String DRAMMER_NOTIFICATION =  BuildConfig.APPLICATION_ID + "broadcast.drammer_notification";
    }

    public interface STATUS {
        public static int STARTED = 0;
        public static int UPDATE = 1;
        public static int FINISHED = 2;
        public static int ERROR = 3;
        public static int BATCH_UPDATE = 4;
        public static int UPLOAD_COMPLETED = 5;
    }

    public interface EXTRAS {
        public static String STATUS = "status";
        public static String MESSAGE = "message";
        public static String START = "start";
        public static String END = "end";
        public static String CMDLINE = "cmdlline";
        public static String EXITCODE = "exitcode";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 42;
    }

    public interface PREFS {
        public static String ISRUNNING = "isRunning";
        public static String RESULT = "result";
        public static String BINARY = "binary";
        public static String OUTPUT = "output";
        public static String UPLOAD = "upload";
        public static String VULNERABLE = "vulnerable";
        public static String DEFRAGTIMEOUT = "defragtimeout";
        public static String IMEI = "imei";
        public static String INSTAlL_UUID = "install_uuid";
        public static String ANDROID_ID = "android_id";
        public static String ADVERTISEMENT_ID = "ad_id";
        public static String MANUFACTURER = "manufacturer";
        public static String MARKET_NAME = "market_name";
        public static String FIRST_RUN = "first_run";
    }
}