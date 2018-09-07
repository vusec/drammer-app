package org.iseclab.drammer;

import java.util.HashMap;

public abstract class Preferences {

    /* Run Drammer binary automatically on launch */
    public static final boolean RUN_AUTOMATICALLY = false;

    /* Show info screen on first run */
    public static final boolean SHOW_INFO_SCREEN = true;

    /* Verbose output to MainActivity */
    public static final boolean VERBOSE_OUTPUT = true;

    /* Keep notification after Drammer service has finished */
    public static final boolean KEEP_NOTIFICATION = false;

    /* Prefer local binary in assets over downloading it?  */
    public static final boolean USE_LOCAL_BINARY = true;

    /* Try to kill background processes before test?  */
    public static final boolean KILL_BACKGROUND_PROCESSES = false;

    /* Hammering timeout in seconds, 0 = no timeout */
    public static final int DRAMMER_TIMEOUT = 0;
    /* Timeouts for automated Espresso tests (in seconds) */
    public static final int ESPRESSO_TIMEOUT = 60*60;
    public static final int ESPRESSO_WAIT_TIMEOUT = 2*60;
    public static final int ESPRESSO_STALL_TIMEOUT = 10;

    /* Timeout for defragmentation during hammering, default = 10 */
    public static final int DRAMMER_DEFRAG_TIMEOUT = 3;
    public static final int DRAMMER_DEFRAG_MAX = 10;

    /* URLs for downloading Drammer binaries and uploading results */
    public static final String DOWNLOAD_URL = "https://www.vvdveen.com/drammer/";
    public static final String UPLOAD_URL = "https://vvdveen.com/drammer/drammer.php";

    /* Filename for log output from Drammer binary */
    public static final String DRAMMER_LOG = "drammer.log";

    /* Number of retries to resolve Advertisement ID from Google Play Services */
    public static final int RETRY_ADID = 10;

    /* Name for shared preferences */
    public static final String DRAMMER_PREFS = BuildConfig.APPLICATION_ID + ".preferences";

    /* Drammer binaries based on architecture, currently only supports ARM */
    public static String DRAMMER_BINARY = null; // set in main depending on architecture
    public static final HashMap<String, String> DRAMMER_BINARIES = new HashMap<String, String>();
    static {
        DRAMMER_BINARIES.put(Constants.ARCH.ARM,     "rh-test");
        DRAMMER_BINARIES.put(Constants.ARCH.ARM64,   "rh-test64");
        DRAMMER_BINARIES.put(Constants.ARCH.x86,     "rh-test-x86");
        DRAMMER_BINARIES.put(Constants.ARCH.x86_64,  "rh-test-x86_64");
    }

}
