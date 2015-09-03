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
package org.mikeneck.gradle.plugin.payara.task;

import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;
import org.mikeneck.gradle.plugin.payara.PayaraPlugin;
import org.mikeneck.gradle.plugin.payara.server.PayaraMicroServer;
import org.mikeneck.gradle.plugin.payara.server.StopServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class AbstractPayaraTask extends ConventionTask {

    private final CountDownLatch latch = new CountDownLatch(1);

    private Integer httpPort;

    private Integer stopPort;

    private String stopCommand;

    private Boolean daemon;

    @TaskAction
    public void runPayara() {
        Logger logger = getClassLogger();
        verifySetting();
        validateConfiguration();
        // create payara micro server
        logger.debug("Creating payara-micro server.");
        Runnable payaraServer = createPayaraMicroServer(latch);
        // create stop server
        logger.debug("Creating StopServer.");
        try (StopServer stopServer = createStopServer()) {
            // acquire ExecutorService
            ExecutorService executors = createExecutors();
            // submit to run payara micro server in ExecutorService
            // submit to run stop server in ExecutorService
            getLogger().lifecycle("Starting payara-micro server.");
            logger.debug("Starting payara-micro server.");
            executors.submit(payaraServer);
            logger.debug("Starting StopServer.");
            executors.submit(stopServer);

            // daemon is true -> finish task
            // daemon is false -> wait for stop server & payara micro server to shutdown
            while (latch.getCount() > 0) {
                try {
                    Thread.sleep(300l);
                } catch (InterruptedException e) {
                    logger.debug("InterruptedException has occurred.", e);
                }
            }
        } catch (Exception e) {
            logger.error("An error has occurred while starting StopServer", e);
            throw new GradleException("Fail to start StopServer", e);
        }
    }

    /**
     * check spec
     * <ul>
     *     <li>All the value is not null.</li>
     *     <li>{@link #httpPort} is not the same value as {@link #stopPort}.</li>
     *     <li>{@link #httpPort} is in the range between {@link org.mikeneck.gradle.plugin.payara.PayaraPlugin#MIN_PORT_NUMBER} abd {@link org.mikeneck.gradle.plugin.payara.PayaraPlugin#MAX_PORT_NUMBER}</li>
     *     <li>{@link #stopPort} is in the range between {@link org.mikeneck.gradle.plugin.payara.PayaraPlugin#MIN_PORT_NUMBER} abd {@link org.mikeneck.gradle.plugin.payara.PayaraPlugin#MAX_PORT_NUMBER}</li>
     *     <li>{@link #stopCommand} is not empty string.</li>
     * </ul>
     */
    private void verifySetting() {
        Logger logger = getClassLogger();
        logger.debug("httpPort -> {}", httpPort);
        logger.debug("stopPort -> {}", stopPort);
        logger.debug("stopCommand -> {}", stopCommand);
        logger.debug("daemon -> {}", daemon);
        if (httpPort == null || stopPort == null || stopCommand == null || daemon == null) {
            throw new InvalidUserDataException("There are null values in httpPort/stopPort/stopCommand/daemon.");
        }
        if (stopCommand.isEmpty()) {
            throw new InvalidUserDataException("stopCommand should be non empty value.");
        }
        portIsInRange("httpPort", httpPort);
        portIsInRange("stopPort", stopPort);
        if (httpPort.equals(stopPort)) {
            throw new InvalidUserDataException("httpPort and stopPort should have different value.");
        }
    }

    private static void portIsInRange(String name, Integer port) {
        if (port < PayaraPlugin.MIN_PORT_NUMBER || PayaraPlugin.MAX_PORT_NUMBER < port) {
            throw new InvalidUserDataException(String.format("%s is not in the range between %d and %d.", name, PayaraPlugin.MIN_PORT_NUMBER, PayaraPlugin.MAX_PORT_NUMBER));
        }
    }

    protected abstract Logger getClassLogger();

    protected abstract void validateConfiguration();

    protected abstract PayaraMicroServer createPayaraMicroServer(CountDownLatch latch);

    protected StopServer createStopServer() throws IOException {
        return new StopServer(stopPort, stopCommand, latch);
    }

    private ExecutorService createExecutors() {
        return Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(daemon);
                return thread;
            }
        });
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getStopPort() {
        return stopPort;
    }

    public void setStopPort(Integer stopPort) {
        this.stopPort = stopPort;
    }

    public String getStopCommand() {
        return stopCommand;
    }

    public void setStopCommand(String stopCommand) {
        this.stopCommand = stopCommand;
    }

    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }
}
