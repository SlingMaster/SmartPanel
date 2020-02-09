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

    CommunicationServer(@NonNull FullscreenActivity fullscreenActivity, int port) {
        activity = fullscreenActivity;
        serverThread = new Thread(new ServerThread(port));
        serverThread.start();
    }

    // =========================================
    // Server
    // =========================================
    class ServerThread implements Runnable {
        int serverPort;

        ServerThread(int port) {
            serverPort = port;
        }

        public void run() {
            try {
                serverSocket = new ServerSocket(serverPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    SessionRunnable sessionRunnable = new SessionRunnable(socket);
                    new Thread(sessionRunnable).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ----------------------------------------------------
    class SessionRunnable implements Runnable {
        Socket clientSocket;
        private BufferedReader in;

        // ===================================
        private SessionRunnable(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // ===================================
        public void run() {
            StringBuilder data = new StringBuilder();
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    if (!line.equals("")) {
                        data.append(line).append('\n');
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                in.close();
                in = null;
                clientSocket.close();
                clientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (activity != null) {
                activity.updateOnUIThread(data.toString());
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
