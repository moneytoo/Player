package com.brouken.player.encrypt;


public final class Constants {
    public static final long ENCRYPT_SKIP = 4096;
    public static final String STREAMER_URL = "http://127.0.0.1:";

    public static final String MAIN_SETTINGS = "MAIN_SETTINGS";
    public static final String KEY_NIGHT_MODE= "KEY_NIGHT_MODE";
    public static final String KEY_PATH= "KEY_PATH";
    public static final String OVERLAY_PERMISSION = "OVERLAY_PERMISSION";
    public static final String KEY_UPDATE = "KEY_UPDATE";
    public static final String KEY_EXPIREDATE = "KEY_EXPIREDATE";
    public static final String KEY_REGISTERDATE = "KEY_REGISTERDATE";
    public static final String KEY_ID = "KEY_ID";
    public static final String KEY_DEVELOPMENT = "KEY_DEVELOPMENT";
    public static final String KEY_VERSION = "KEY_VERSION";
    public static final String KEY_MEMBER = "KEY_MEMBER";
    public static final String KEY_CAST = "KEY_CAST";
    public static final String KEY_RENDER = "KEY_RENDER";
    public static final String KEY_PLAYER = "KEY_PLAYER";
    public static final String KEY_POSITION = "KEY_POSITION";
    public static final String KEY_TITLE = "KEY_TITLE";
    public static final String KEY_IMAGE_FILL = "KEY_IMAGE_FILL";
    public static final String KEY_CACHE = "KEY_CACHE";
    public static final String KEY_PW = "KEY_PW";
    public static final String KEY_MODE = "KEY_MODE";
    public static final String KEY_PIP = "KEY_PIP";
    public static final String KEY_UNLOCK = "KEY_UNLOCK";
    public static final String USAGE_COUNT = "USAGE_COUNT";
    public static final String KEY_REGISTRATION = "KEY_REGISTRATION";
    public static final String KEY_ENCRYPT = "KEY_ENCRYPT";
    public static final String KEY_DIR_DOC = "KEY_DIR_DOC";
    public static final String KEY_UNIQUE_ID = "PREF_UNIQUE_ID";
    public static final String KEY_RNDAMOUNT = "KEY_RNDAMOUNT";
    public static final String KEY_BALANCE = "KEY_BALANCE";
    public static final String KEY_INSTALLATION = "KEY_INSTALLATION";
    public static final String KEY_ENDING = "KEY_ENDING";
    public static final String KEY_REGISTED = "KEY_REGISTED";
    public static final String KEY_ALLOWED = "KEY_ALLOWED";
    public static final String KEY_SHUFFLE = "KEY_SHUFFLE";
    public static final String KEY_FLOAT = "KEY_FLOAT";
    public static final String KEY_VIDEODIRECT = "KEY_VIDEODIRECT";
    public static final String URL_PATH_KEY = "URL_PATH_KEY";
    public static final String URL_MIMETYPE_KEY = "URL_MIMETYPE_KEY";
    public final static int LIST_REQUEST = 99;
    public final static int PERMISSIONS_REQUEST = 0;
    public final static int VIDEO_REQUEST = 1;
    public static final String VIDEO_INDEX = "VIDEO_INDEX";
    public static final String VIDEO_POSITION = "VIDEO_POSITION";
    public static final String VIDEO_LIST = "VIDEO_LIST";
    public static final String VIDEO_DETECT = "VIDEO_DETECT";
    public static final String VIDEO_GROUP = "VIDEO_GROUP";
    public static final String ACTION_LISTENER = "ACTION_LISTENER";
    public static int OVERLAY_PERMISSION_REQ = 1234;
    public static final int REQUEST_DRAWOVERLAYS_CODE = 10000;
    public static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    public static final String KEY_SECURITY_ENABLED = "security_enable";
    public static String AES_ALGORITHM = "AES";
    public static String CTR_TRANSFORMATION = "AES/CTR/NoPadding";
    public static String MAIN_PASSWORD = "1962121219580610";  //use the date of app.version release;
    public static String P_IV = "1962121219580610";  //use the date of app.version release;
    public static final int DEFAULT_BUFFER_SIZE =  1024*1024;//best size
    public static String THREAD_ENCRYPTION="THREAD_ENCRYPTION";
    //online options mProcess
    public static int EXTRACT_ONLY = 0;
    public static int LINK_PLAY = 1;
    public static int LINK_DOWN = 2;  //must be same as DOWN_ONLY
    public static int LIST_UPDATE = 3;
    public static int EN_DOWNLOAD = 4;
    public static int APK_DOWNLAOD = 5;


    //downloading options
    public final static int D_PLAY_ONLY = 0; //direct or encrypted play but no download
    public final static int D_PLAY_DOWN = 1;  //direct play and direct download
    public final static int D_DOWN_ONLY = 2;  //direct download
    public final static int D_PLAY_ENDOWN = 3;  //direct play and ENCRYPTED download
    public final static int D_ENDOWN_ONLY = 4;  //ENCRYPTED download


    public final static int LIST_GROUP = 0;
    public final static int LIST_ONLINE = 1;

    public final static String MESSAGE = "MESSAGE";
    public final static String APP_BROADCAST = "APP_BROADCAST";
    public final static String LIST_BROADCAST = "LIST_BROADCAST";
    public final static String DOWNLOAD_BROADCAST = "DOWNLOAD_BROADCAST";
    public final static String DOWNLOAD_URL = "url";
    public final static String DOWNLOAD_FULL_NAME = "name";

    //---------feedback and enquiry
    public final static String REGISTRATION = "REGISTRATION";
    public final static String DONATENYTAIJI = "DONATENYTAIJI";
    public final static String REQUESTREFUND = "REQUESTREFUND";
    public final static String UNLOCKVIDEO = "UNLOCKVIDEO";
    public final static String REPAIRCOMMAND = "REPAIRCOMMAND";
    public final static String MIGRATIONREQ = "MIGRATIONREQ";

    //username sample
    //SH3MHWH1
    //userdefault password SH3MHWH1sh3mhwh1
    public static final String LEVEL0_DEFAULT = "20090418nanyangA";


    //-----------------update------------
    public static final String EXTRA_APP_NAME = "extra_appName";
    public static final String EXTRA_VERSION_NAME = "extra_versionName";
    public static final String EXTRA_APP_LINK = "extra_appLink";

    public static final String LIST_RECALL = "LIST_RECALL";
    public static final String LIST_BLACK = "LIST_BLACK";
    public static final String LIST_UNLOCK = "LIST_UNLOCK";

    //-------------
    public static final String CHANNEL_ID = "CHANNEL_ID";
    public static final String NANYANG_CHANNEL = "Nanyang Taiji";
    public static final String ACTION_MAIN = "MAIN_ACTION";
    public static final String ACTION_PREV = "PREV_ACTION";
    public static final String ACTION_NEXT = "NEXT_ACTION";
    public static final String ACTION_PLAY = "PLAY_ACTION";
    public static final String ACTION_PAUSE = "PAUSE_ACTION";
    public static final String ACTION_SET_SHUFFLE_MODE = "SHUFFLE_MODE";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_STOP = "STOP_ACTION";
    public static final String ACTION_START_FOREGROUND = "START_FOREGROUND_ACTION";
    public static final String ACTION_STOP_FOREGROUND = "STOP_FOREGROUND_ACTION";
    public static final String ACTION_REWIND = "REWIND_ACTION";
    public static final String ACTION_FAST_FORWARD = "FAST_FORWARD_ACTION";
    public static final String STATE_BIND = "STATE_BIND";

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }

}


