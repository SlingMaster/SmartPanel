/*
 * Copyright (c) 2020
 * Jeneral Samopal Company
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;

class Constants {
    static final int SERVER_PORT = 8080;
    // public static final int PERMISSION_REQUEST_CODE = 101;
    // public static final int PERMISSION_CHANGE_TIMEOUT = 10000;
    final static int REQUEST_ACTIVITY_CODE = 1;

    // command by int ------
    static final int CMD_HOME = 0x00;
    static final int CMD_RESTART = 0x01;
    static final int CMD_BACK = 0x03;
    static final int CMD_BACK_LIGHT = 0x04;
    static final int CMD_AUTO_SWAP = 0x05;
    static final int CMD_EXIT = 0x07;
    static final int CMD_DEBUG_MODE = 0x09;

    // main menu cmd code ------------------------
    static final int CMD_RADIO = 0x0A;
    static final int CMD_LOAD_SMART = 0x14;
    static final int CMD_LOAD_STATS = 0x1E;

    static final int CMD_LOAD_TIMER = 0x28;
    /*--*/ static final int CMD_TIMER_SWAP = 0x29;
    // -------------------------------------------
    static final int CMD_LOAD_WEATHER = 0x32;
    /*--*/ static final int CMD_WEATHER_FORECAST = 0x33;
    /*--*/ static final int CMD_WEATHER_MAGIC = 0x34;
    // -------------------------------------------
    static final int CMD_SLING = 0x3C;
    static final int CMD_WIFI_SCANNER = 0x46;
    static final String[] HTML_APPS = {"smarthome.html", "smarthome/statistic.html", "timer.html", "weather.html"};
    static final String[] PACKAGES = {"com.jsc.smartpanel", "air.InternetRadio", "air.SlingPlayerTablet3.A4", "com.pinapps.amped"};
    static final int[] SWAP_APPS = {CMD_LOAD_TIMER, CMD_LOAD_WEATHER, CMD_LOAD_SMART, CMD_LOAD_STATS};
    // intent action ----------------------------
    static final String SWAP_SCREEN = "swap_screen";
    static final String LOAD_SCREEN = "load_screen";
    static final String SYNC = "sync";
    static final String WOKE_UP = "woke_up";
    static final String SHOW_TIME = "show_time";
    // applicationn state -----------------------
    static final int MAIN = 0;
    static final int INTERNAL = 1;
    static final int EXTERNAL = 2;
}

