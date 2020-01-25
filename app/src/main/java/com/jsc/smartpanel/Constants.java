/*
 * Copyright (c) 2018. by RFControls. All Rights Reserved.
 * www.http://rfcontrols.com/
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;
// @SuppressWarnings("WeakerAccess")

public class Constants {
    public static final int MAX_SCR = 2;
    public static final int PERMISSION_REQUEST_CODE = 101;
    public static final int PERMISSION_CHANGE_TIMEOUT = 10000;
    public static final int BYTE_SIZE = 16;
    public static final byte START = 0x01;
    public static final short DELIMITER = 0xC0;

    // static final String SOCKET_OPEN = "CONNECT";
    // static final String SOCKET_CLOSE = "DISCONNECT";


    public static final String VAL_C0 = "C0";
    public static final String VAL_DB = "DB";
    public static final String VAL_DC = "DC";
    public static final String VAL_DD = "DD";

    // command by int ------
    static final int CMD_RESTART            = 0x01;
    static final int CMD_BACK               = 0x03;
    static final int CMD_BACK_LIGHT         = 0x04;
    static final int CMD_DEBUG_MODE         = 0x09;

    // main menu cmd code ------------------------
    static final int CMD_RADIO              = 0x0A;
    static final int CMD_LOAD_SMART         = 0x14;
    static final int CMD_LOAD_STATS         = 0x1E;

    static final int CMD_LOAD_TIMER         = 0x28;
    /*--*/ static final int CMD_TIMER_SWAP          = 0x29;
    // -------------------------------------------
    static final int CMD_LOAD_WEATHER       = 0x32;
    /*--*/ static final int CMD_WEATHER_FORECAST    = 0x33;
    /*--*/ static final int CMD_WEATHER_MAGIC       = 0x34;
    // -------------------------------------------
    static final int CMD_SLING              = 0x3C;

    //static final int CMD_TEST             = 0x7D;
    // ---------------------
}


