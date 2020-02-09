/*
 * Copyright (c) 2020
 * Jeneral Samopal Company
 * Programming by Alex Uchitel
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

class CommunicationServer {
    private FullscreenActivity activity;
    private ServerSocket serverSocket;
    private Thread serverThread;

    CommunicationServer(@NonNull FullscreenActivity fullscreenActivity) {
        activity = fullscreenActivity;
        serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    // =========================================
    // Server
    // =========================================
    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ----------------------------------------------------
    class CommunicationThread implements Runnable {
        Socket clientSocket;
        private BufferedReader in;

        // ===================================
        private CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // ===================================
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String readData = in.readLine();
                    if (!readData.equals("")) {
                        if (activity != null) {
                            activity.updateOnUIThread(readData);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    void stop() {
        try {
            if (serverThread != null) {
                serverThread.interrupt();
                serverThread = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
            activity = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
