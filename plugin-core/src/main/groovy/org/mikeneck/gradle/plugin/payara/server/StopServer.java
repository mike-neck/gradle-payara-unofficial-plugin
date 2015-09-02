/*
 * Copyright 2015 Shinya Mochida
 * 
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,software
 * Distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mikeneck.gradle.plugin.payara.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.mikeneck.gradle.plugin.payara.PayaraPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Because payara micro doesn't have stop command, stop port, this server is an alternative for it, using {@link com.sun.net.httpserver.HttpServer}.
 */
public class StopServer implements Runnable, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StopServer.class);

    public static final int OK = 200;

    public static final int BAD_REQUEST = 400;

    public static final int METHOD_NOT_ALLOWED = 405;

    private final HttpServer server;

    private final String stopCommand;

    private final CountDownLatch latch;

    private final CountDownLatch internal = new CountDownLatch(1);

    public StopServer(int stopPort, String stopCommand, CountDownLatch latch) throws IOException {
        this.stopCommand = stopCommand;
        this.latch = latch;
        InetSocketAddress address = new InetSocketAddress("localhost", stopPort);
        server = HttpServer.create(address, stopPort);
    }

    @Override
    public void run() {
        LOG.info("Starting StopServer...");
        Handler handler = new Handler();
        server.createContext("/", handler);
        server.start();
        LOG.info("StopServer started.");
        try {
            internal.await();
        } catch (InterruptedException e) {
            LOG.info("StopServer is going to shutdown unexpectedly.");
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("Stopping StopServer...");
        server.stop(0);
        LOG.debug("StopServer is stopped.");
    }

    private class Handler implements HttpHandler {

        private static final String UTF8 = "UTF-8";

        private static final String CONTENT_TYPE = "Content-Type";

        private static final String TEXT_PLAIN = "text/plain; charset=UTF-8";

        @Override
        public void handle(HttpExchange http) throws IOException {
            String method = http.getRequestMethod();
            try(BufferedReader reader = toReader(http.getRequestBody())) {
                StringBuilder sb = new StringBuilder();
                while (reader.ready()) {
                    sb.append(reader.readLine());
                }
                String command = sb.toString();
                boolean validMethod = PayaraPlugin.STOP_METHOD.equalsIgnoreCase(method);
                // receive POST method and valid stopCommand then terminate PayaraMicro/StopServer
                if(validMethod && stopCommand.equals(command)) {
                    LOG.debug("stopCommand coming.");
                    latch.countDown();
                    sendMessage(http, OK, "Server is going to shutdown.\n");
                    internal.countDown();
                // receive POST bad invalid stopCommand send BAD_REQUEST
                } else if (validMethod) {
                    sendMessage(http, BAD_REQUEST, "Stop command is different.\n");
                // receive another method send METHOD_NOT_ALLOWED
                } else {
                    sendMessage(http, METHOD_NOT_ALLOWED, "Method is different.\n");
                }
            }
        }

        private BufferedReader toReader(InputStream st) {
            return new BufferedReader(new InputStreamReader(st));
        }

        private void sendMessage(HttpExchange http, int status, String msg) throws IOException {
            byte[] bytes = msg.getBytes(UTF8);
            http.getResponseHeaders().add(CONTENT_TYPE, TEXT_PLAIN);
            http.sendResponseHeaders(status, bytes.length);
            try(OutputStream body = http.getResponseBody()) {
                body.write(bytes);
                body.flush();
            }
        }
    }
}
