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
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;
import org.mikeneck.gradle.plugin.payara.PayaraPlugin;
import org.mikeneck.gradle.plugin.payara.server.StopServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PayaraStop extends ConventionTask {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraStop.class);

    public static final String TASK_NAME = "payaraStop";

    public static final String DESCRIPTION = "Stops payara-micro.";

    private Integer stopPort;

    private String stopCommand;

    @TaskAction
    public void stopPayara() {
        String urlString = String.format("http://localhost:%s/", stopPort);
        HttpURLConnection con = null;
        BufferedReader reader;
        try {
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(PayaraPlugin.STOP_METHOD);
            con.setDoInput(true);
            OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
            writer.append(stopCommand).flush();
            int status = con.getResponseCode();
            if (status != StopServer.OK) {
                throw new GradleException("Cannot stop server.");
            }
        } catch (MalformedURLException e) {
            throw new GradleException(String.format("Invalid stop server url[%s].", urlString));
        } catch (IOException e) {
            throw new GradleException("An error occurred while connecting to StopServer.");
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
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
}
