/*
 * Copyright (c) 2018. by RFControls. All Rights Reserved.
 * www.http://rfcontrols.com/
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel.html;

public class JSConstants {

    public static final String INTERFACE_NAME = "NativeApplication";
    // ---------------------------------------------
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    // ---------------------------------------------

    // events from client --------
    public static final int EVT_MAIN_TEST = 0;
    public static final int EVT_READY = 1;
    public static final int EVT_BACK = 4;
    // public static final int EVT_SYNC = 5;
    // ---------------------------

    // events from ui app client --
    public static final int EVT_FM_LIST = 1100;
    public static final int EVT_SAVE_PREFS = 1101;
    public static final int EVT_MUTE = 1102;

    public static final int EVT_WEATHER = 5000;
    public static final int EVT_WOKE_UP = 5100;
    // public static final int EVT_NEXT = 5101;
    public static final int EVT_SHOW_TIME = 5103;
    // ---------------------------

    // command for client --------
    public static final int CMD_INIT = 1000;

    public static final int CMD_WEATHER_DATA = 2000;
    public static final int CMD_SEASON_MAGIC = 2001;
    public static final int CMD_SWAP = 2100;

    public static final int CMD_FM_DATA = 1110;
    public static final int CMD_RADIO_PREV = 1115;
    public static final int CMD_RADIO_MUTE = 1116;
    public static final int CMD_RADIO_NEXT = 1117;

    public static final int CMD_RADIO_FAV1 = 1111;
    public static final int CMD_RADIO_FAV2 = 1112;
    public static final int CMD_RADIO_FAV3 = 1113;
    public static final int CMD_RADIO_FAV4 = 1114;
    // ---------------------------
    public static final int CMD_EXO = 777;
    // events  app ---------------
    public static final int EVT_PAGE_FINISHED = 889;
    // ---------------------------
}
