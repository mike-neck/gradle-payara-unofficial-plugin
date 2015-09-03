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

import fish.payara.micro.PayaraMicro;
import org.gradle.api.InvalidUserDataException;
import org.mikeneck.gradle.plugin.payara.server.PayaraMicroServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class PayaraRunWar extends AbstractPayaraTask {

    public static final String TASK_NAME = "payaraRunWar";

    public static final String DESCRIPTION = "Assembles the JavaEE app into a war and deploys it to payara-micro.";

    private static final Logger LOG = LoggerFactory.getLogger(PayaraRunWar.class);

    private File war;

    @Override
    protected Logger getClassLogger() {
        return LOG;
    }

    @Override
    public void validateConfiguration() {
        if (war == null || !war.exists()) {
            throw new InvalidUserDataException("Invalid configuration. Web archive file is not set.");
        }
    }

    @Override
    protected PayaraMicroServer createPayaraMicroServer(CountDownLatch latch) {
        return new PayaraMicroServer(getHttpPort(), latch) {
            @Override
            protected PayaraMicro createPayaraMicro() {
                return PayaraMicro.getInstance()
                        .setHttpPort(getHttpPort())
                        .addDeployment(war.getAbsolutePath());
            }
        };
    }

    public File getWar() {
        return war;
    }

    public void setWar(File war) {
        this.war = war;
    }
}
