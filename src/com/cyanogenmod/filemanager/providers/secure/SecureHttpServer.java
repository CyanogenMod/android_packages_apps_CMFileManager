/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.cyanogenmod.filemanager.providers.secure;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * SecureHttpServer
 * <pre>
 *    This is NOT HTTPS!  This is a simple socket server to all a connection and spoofs an HTTP
 *    server that pipes the data of the secure file back to the calling application.
 *
 *    The application must know how to stream http data from a server in order to access the
 *    ssh data.
 *
 *    This allows us to handle transferring files larger than 1MB, whereas the {@link
 *    com.cyanogenmod.filemanager.providers.SecureResourceProvider} can only handle up to 1MB
 *    because it is using RPC which has a transfer limitation on size.
 * </pre>
 */
public class SecureHttpServer {

    // Constants
    private static final String TAG = SecureHttpServer.class.getSimpleName();
    private static final boolean DEBUG =
            Log.isLoggable(TAG, Log.DEBUG) || "eng".equals(Build.TYPE) ||
                    "userdebug".equals(Build.TYPE);

    // Thread pool
    private static ExecutorService sThreadPool = Executors.newCachedThreadPool();

    /**
     * SecureFileSocket
     * <pre>
     *    This is a socket for piping a file to a client
     * </pre>
     * @see {@link java.lang.Runnable}
     */
    private class SecureFileSocket implements Runnable {

        private static final int PORT = 8000; // [TODO][MSB]: Randomize to allow multiple
        // simultaneous operations

        // Members
        private File mFile;
        private long mTimeout;
        private ServerSocket mSocket;
        private Socket mClientSocket;
        private Future mFuture;

        /**
         * Constructor
         *
         * @param file {@link java.io.File}
         * @param timeout {@link java.lang.Long}
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        public SecureFileSocket(File file, long timeout) throws IllegalArgumentException {
            if (file == null) {
                throw new IllegalArgumentException("'file' cannot be null!");
            }
            mFile = file;
            mTimeout = timeout;
        }

        /**
         * Get the file object
         *
         * @return {@link java.io.File}
         */
        public File getFile() {
            return mFile;
        }

        /**
         * Get the timeout
         *
         * @return {@link java.lang.Long}
         */
        public long getTimeout() {
            return mTimeout;
        }

        /**
         * Start a connection thread
         */
        public void start() {
            if (mFuture == null) {
                mFuture = sThreadPool.submit(this);
            }
        }

        /**
         * Stop the connection thread from running
         */
        public void stop() {
            if (mFuture != null) {
                mFuture.cancel(true);
                mFuture = null;
            }
        }

        @Override
        public void run() {
            OutputStream outputStream = null;
            try {
                mSocket = new ServerSocket(PORT);
                log("Opening socket on localhost:" + mSocket.getLocalPort());
                mSocket.setSoTimeout((int) mTimeout);
                mClientSocket = mSocket.accept();
                log("Socket accept from " + mClientSocket.getInetAddress());
                mFile = new File(Environment
                        .getExternalStorageDirectory(), "16.mp3");
                Log.i("TEST", "Path: " + mFile.getAbsolutePath());
                Log.i("TEST", "Exists: " + mFile.exists());
                FileInputStream fis = new FileInputStream(mFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] fileData = new byte[(int) mFile.length()];
                int bytesRead = bis.read(fileData, 0, (int) mFile.length());
                log("Bytes Read from file: " + bytesRead);
                outputStream = mClientSocket.getOutputStream();
                StringBuilder sb = new StringBuilder();
                sb.append( "HTTP/1.1 200 OK\r\n");
                sb.append( "Content-Type: audio/mpeg\r\n"); // [TODO][MSB]: Dynamic mime type
                sb.append( "Connection: close\r\n" );
                sb.append( "Accept-Ranges: bytes\r\n" );
                sb.append( "Content-Length: ").append(mFile.length()).append("\r\n");
                sb.append("Content-Disposition: inline; filename=")
                        .append(mFile.getName())
                        .append("\r\n\r\n");
                log("Header:\n" + sb.toString());
                outputStream.write(sb.toString().getBytes());
                outputStream.write(fileData, 0, fileData.length);
                // [TODO][MSB]:
                // May need to show a dialog at some point and say "preparing for share"
                // until the file is in memory.  Need to figure out how the secure stuff
                // works a little more under the hood
                // read encrypted file
            } catch (IOException e) {
                log(e.getMessage(), e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                    } catch (IOException e) {
                        log(e.getMessage(), e);
                    }
                }
                if (mClientSocket != null) {
                    try {
                        mClientSocket.close();
                    } catch (IOException e) {
                        log(e.getMessage(), e);
                    } finally {
                        mClientSocket = null;
                    }
                }
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        log(e.getMessage(), e);
                    } finally {
                        mSocket = null;
                    }
                }
            }
        }
    }

    /**
     * Log a message
     *
     * @param msg {@link java.lang.String}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    private static void log(String msg) throws IllegalArgumentException {
        log(msg, null);
    }

    /**
     * Log a message
     *
     * @param msg {@link java.lang.String}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    private static void log(String msg, Throwable throwable) throws IllegalArgumentException {
        if (TextUtils.isEmpty(msg)) {
            throw new IllegalArgumentException("'msg' cannot be null or empty!");
        }
        if (DEBUG) {
            Log.d(TAG, msg, throwable);
        }
    }

    // Instance
    private static SecureHttpServer sInstance = null;

    // Members
    private static final Map<String, SecureFileSocket> sFileSocketMap = new HashMap<String,
            SecureFileSocket>(); //<String filename, Target Socket>
    private WeakReference<Context> mContext;

    /**
     * Constructor
     */
    private SecureHttpServer(Context context) {
        mContext = new WeakReference<Context>(context);
        sFileSocketMap.clear();
    }

    /**
     * Create a new instance or get the existing instance
     *
     * @return {@link com.cyanogenmod.filemanager.providers.secure.SecureHttpServer}
     */
    public static SecureHttpServer createInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SecureHttpServer(context);
        }
        return sInstance;
    }

    /**
     * This will open a secure file socket for retrieving the file data.
     *
     * @param file {@link java.io.File}
     * @param timeout {@link java.lang.Long}
     *
     * @return {@link java.lang.Boolean}
     *
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public boolean openSecureFileSocketForFile(File file, long timeout)
            throws IllegalArgumentException {
        if (file == null) {
            throw new IllegalArgumentException("'file' cannot be null!");
        }
        SecureFileSocket secureFileSocket = new SecureFileSocket(file, timeout);
        secureFileSocket.start();
        sFileSocketMap.put(file.getAbsolutePath(), secureFileSocket);
        return true;
    }

}
