/*
 * Copyright (c) 2020.
 * RF Controls
 * Design and Programming by Alex Dovby
 */

package com.jsc.smartpanel;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import utils.PackageCreator;

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
        // private BufferedReader input;
        // -----------------------------------
        /* создаем буфер для данных */
        byte[] buffer;
        //private InputStream in;
        private BufferedReader in;
        // -----------------------------------

        // public CommunicationThread(Socket clientSocket) {
        private CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                this.buffer = new byte[16];
                //this.in = this.clientSocket.getInputStream();
                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                // this.in = this.clientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    String read = in.readLine();

                    // int countS = in.read(buffer, 0, buffer.length);
                    if (!read.equals("")) {
//                    if (countS > 0) {
                        // decode data header, restore src CO and DB ----
                        //byte[] header;
                        //header = PackageCreator.decodePackage(PackageCreator.copyPartArray(buffer, 0, countS));
                        //String headerStr = PackageCreator.getHeaderStr(header);
                        //System.out.println("========== TcpClient header length: " + header.length + " | headerStr : " + headerStr );
                        if (activity != null) {
                            //activity.updateOnUIThread(headerStr);
                            activity.updateOnUIThread(read);
//                            clientSocket.close();
//                            Thread.currentThread().interrupt();
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
