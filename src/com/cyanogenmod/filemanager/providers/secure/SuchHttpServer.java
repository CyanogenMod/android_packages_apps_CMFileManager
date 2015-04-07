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

import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.cyanogenmod.filemanager.console.CommandNotFoundException;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.OperationTimeoutException;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.RegularFile;
import com.cyanogenmod.filemanager.providers.SecureResourceProvider;
import com.cyanogenmod.filemanager.util.CommandHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class SuchHttpServer {

    // Constants
    private static final String TAG = SuchHttpServer.class.getSimpleName();
    private static final boolean DEBUG =
            Log.isLoggable(TAG, Log.DEBUG) || "eng".equals(Build.TYPE) ||
                    "userdebug".equals(Build.TYPE);
    private static final int PORT = 8000;
    private static final int SOCKET_TIMEOUT = 20000; // 20 seconds, same as the auth timeout

    // Thread pool
    private static ExecutorService sThreadPool = Executors.newCachedThreadPool();

    /**
     * Response
     */
    private class Response {

        // Members
        private List<String> mHeaders = new LinkedList<String>();
        private URL mUrl;

        /**
         * Add a header line
         *
         * @param header {@link java.lang.String}
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        public void addHeader(String header) throws IllegalArgumentException {
            if (TextUtils.isEmpty(header)) {
                throw new IllegalArgumentException("'header' cannot be null or empty!");
            }
            mHeaders.add(header);
        }

        /**
         * Set the URL
         *
         * @param url {@link java.net.URL}
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        public void setUrl(URL url) throws IllegalArgumentException {
            if (url == null) {
                throw new IllegalArgumentException("'url' cannot be null!");
            }
            mUrl = url;
        }

        /**
         * Get the headers
         *
         * @return {@link java.lang.String}
         */
        public List<String> getHeaders() {
            return mHeaders;
        }

        /**
         * Get the url
         *
         * @return {@link java.net.URL}
         */
        public URL getURL() {
            return mUrl;
        }

    }

    private class FileReadListener implements AsyncResultListener {

        // Members
        private Socket mClientSocket;
        private PrintStream mPrintStream;
        private Response mResponse;

        /**
         * Constructor
         *
         * @param clientSocket {@link java.net.Socket}
         */
        public FileReadListener(Socket clientSocket, Response response) {
            mClientSocket = clientSocket;
            mResponse = response;
            try {
                mPrintStream = new PrintStream(mClientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAsyncStart() {
            if(mPrintStream != null) {
                // Write our header data
                log("Writing header data...");
                for (String header : mResponse.getHeaders()) {
                    mPrintStream.print(header);
                    mPrintStream.print("\r\n");
                }

                // Terminate header
                mPrintStream.print("\r\n");
            }
        }

        @Override
        public void onAsyncEnd(boolean cancelled) {
            if (mPrintStream != null) {
                // Terminate data
                mPrintStream.print("\r\n");
                // Flush buffer
                mPrintStream.flush();
            }
            if (mClientSocket != null) {
                try {
                    mClientSocket.close();
                } catch (IOException e) {
                    log(e.getMessage(), e);
                }
            }
            log("Done");
        }

        @Override
        public void onAsyncExitCode(int exitCode) {

        }

        @Override
        public void onPartialResult(Object result) {
            if (result == null) {
                return;
            }
            byte[] buffer = (byte[]) result;
            mPrintStream.write(buffer, 0, buffer.length);
        }

        @Override
        public void onException(Exception cause) {

        }
    }

    private class RequestHandler implements Runnable {

        // Members
        private Socket mClientSocket;

        /**
         * Constructor
         *
         * @param socket {@link java.net.Socket} the client socket
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        public RequestHandler(Socket socket) throws IllegalArgumentException {
            if (socket == null) {
                throw new IllegalArgumentException("'socket' cannot be null!");
            }
            log("Initializing handler...");
            mClientSocket = socket;
        }

        @Override
        public void run() {
            try {

                // Derive path
                Scanner scanner = new Scanner(mClientSocket.getInputStream(), "US-ASCII");
                String line = getPath(scanner.nextLine());
                String path;
                String packageName = null;
                if (line != null && line.contains("?")) {
                    path = line.split("\\?")[0];
                    String query = line.split("\\?")[1];
                    if (query != null) {
                        packageName = query.split("=")[1];
                    }
                } else {
                    path = line;
                }
                log("Derived path: " + path);

                // Get uri UUID which should be the path?
                Uri tmpUri = Uri.parse(path);
                FileSystemObject fso = SecureResourceProvider
                        .getFileSystemObject(tmpUri, packageName);
                path = fso.getName();

                // Create a response
                Response response = find(path);

                                // Start reading data
                CommandHelper.read(FileManagerApplication.getInstance().getApplicationContext(),
                        fso.getFullPath(), new FileReadListener(mClientSocket, response), null);

            } catch (IOException e) {
                log(e.getMessage(), e);
            } catch (ConsoleAllocException e) {
                e.printStackTrace();
            } catch (NoSuchFileOrDirectory noSuchFileOrDirectory) {
                noSuchFileOrDirectory.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InvalidCommandDefinitionException e) {
                e.printStackTrace();
            } catch (CommandNotFoundException e) {
                e.printStackTrace();
            } catch (OperationTimeoutException e) {
                e.printStackTrace();
            } catch (InsufficientPermissionsException e) {
                e.printStackTrace();
            }

        }

        /**
         * Parse out the path from the request line
         *
         * @param line {@link java.lang.String}
         *
         * @return {@link java.lang.String}
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        private String getPath(String line) throws IllegalArgumentException {
            if (TextUtils.isEmpty(line)) {
                throw new IllegalArgumentException("'line' cannot be empty or null!");
            }
            Matcher matcher = Pattern.compile("GET (/\\S*) HTTP/1\\.1").matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
            throw new IllegalArgumentException("incorrect request line!");
        }

        /**
         * Build the response to serve the file to the client
         *
         * @param path {@link java.lang.String}
         *
         * @return {@link SuchHttpServer.Response}
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        private Response find(String path) throws IllegalArgumentException {
            if (TextUtils.isEmpty(path)) {
                throw new IllegalArgumentException("'path' cannot be null or empty!");
            }
            path = ("/".equals(path)) ? "/index.html" : path;
            path = (path.startsWith("/") ? path : "/" + path);
            Response response = new Response();
            URL url = null;
            try {
                url = new URL("http://127.0.0.1:8000" + path); // [TODO][MSB]: Hard coded address
            } catch (MalformedURLException e) {
                log(e.getMessage(), e);
            }
            log("URL="+url);
            if (url == null) {
                response.addHeader("HTTP/1.1 404 NOT FOUND");
            } else {
                response.setUrl(url);
                response.addHeader("HTTP/1.1 200 OK");
                String type = "application/octet-stream";
                String extension = url.toString();
                // [TODO][MSB]: parse mime type
                if (extension.endsWith(".mp3")) {
                    type = "audio/mp3";
                }
                response.addHeader("Content-Type: " + type);
            }
            return response;
        }

   }

    /**
     * ServeRunnable
     * <pre>
     *     Listens on the port
     * </pre>
     */
    private class ServeRunnable implements Runnable {

        // Members
        private int mPort;

        // Flags
        private boolean mIsListening = false;

        /**
         * Constructor
         *
         * @param port {@link java.lang.Integer}
         */
        public ServeRunnable(int port) {
            mPort = port;
        }

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                log("Creating socket on port: " + mPort);
                serverSocket = new ServerSocket(mPort);
                serverSocket.setSoTimeout(SOCKET_TIMEOUT);
                while (true) {
                    log("Waiting for client...");
                    final Socket client = serverSocket.accept();
                    log("Client " + client.getInetAddress() + " has connected...");
                    sThreadPool.submit(new RequestHandler(client)); // [TODO][MSB]: Keep handle for
                    // cancelling
                }
            } catch (IOException ioe) {
                log(ioe.getMessage(), ioe);
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        log(e.getMessage(), e);
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
    private static SuchHttpServer sInstance = null;

    // Members
    private Future mServerFuture;

    /**
     * Constructor
     */
    private SuchHttpServer() {
    }

    /**
     * Create a new instance or get the existing instance
     *
     * @return {@link SuchHttpServer}
     */
    public static SuchHttpServer createInstance() {
        if (sInstance == null) {
            sInstance = new SuchHttpServer();
        }
        return sInstance;
    }

    /**
     * Start listening on the port
     *
     * @throws IOException {@link java.io.IOException}
     */
    public void startListening() throws IOException {
        if (mServerFuture == null || mServerFuture.isDone() || mServerFuture.isCancelled()) {
            log("Start listening...");
            mServerFuture = sThreadPool.submit(new ServeRunnable(PORT));
        }
    }

    /**
     * Stop listening on the port
     *
     * @throws IOException {@link java.io.IOException}
     */
    public void stopListening() throws IOException {
        if (mServerFuture != null && (!mServerFuture.isCancelled() || !mServerFuture.isDone())) {
            log("Stop listening...");
            mServerFuture.cancel(true);
            mServerFuture = null;
        }
    }

}
