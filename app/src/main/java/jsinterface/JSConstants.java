/*
 * Copyright (c) 2018. by RFControls. All Rights Reserved.
 * www.http://rfcontrols.com/
 * Design and Programming by Alex Dovby
 */

package jsinterface;

public class JSConstants {
    // ---------------------------------------------
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    // ---------------------------------------------
    public static final String INTERFACE_NAME = "NativeApplication";


    // events from client --------
    public static final int EVT_MAIN_TEST = 0;
    public static final int EVT_READY = 1;
    public static final int EVT_BACK = 4;

    // events from ui app client --
    public static final int EVT_WEATHER = 5000;
//    public static final int EVT_UI_PROJECT = 21;
//    public static final int EVT_UI_RETURN = 22;
    // ---------------------------

    // command for client --------
    public static final int CMD_INIT = 1000;
    public static final int CMD_WEATHER_DATA = 2000;
    public static final int CMD_SEASON_MAGIC = 2001;
    public static final int CMD_SWAP = 2100;
    public static final int EVT_NEXT = 5101;
    public static final int EVT_BACK_LIGHT = 5102;
    // send data to html ui ------

    public static final int CMD_STATISTICS_DATA = 5001;
    public static final int CMD_CONTROL_DATA = 5002;
    // events cur app ------------
    public static final int EVT_WEATHER_DATA = 6000;

    public static final int EVT_NIGHT_MODE = 5100;

    //    public static final int EVT_STATISTICS_DATA = 6001;
    public static final int EVT_THERMOSTATS_CONTROL = 6002;
    public static final int EVT_SEND_CHANNEL_ID = 6005;

    public static final int CMD_BACK = 4;
    //public static final int CMD_SWAP = 5;
    //    public static final int EVT_TARGET_GEOPOSITION = 5;
//    public static final int CMD_TARGET_COMPLETE = 6;
//    public static final int CMD_UPDATE_GEOPOSITION = 7;
//    public static final int CMD_AUTO_UPDATE_GEOPOSITION = 8;
//    public static final int CMD_TARGET_GEOPOSITION = 9;
//    public static final int EVT_NAVIGATION = 10;
//    public static final int CMD_REDIRECT = 32;
    public static final int EVT_EXIT = 35;
    public static final int CMD_EXO = 700;
    public static final int EVT_EXO = 777;
    public static final int EVT_EXO_RESPONSE = 888;
}
