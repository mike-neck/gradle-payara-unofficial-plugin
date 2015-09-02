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

import fish.payara.micro.BootstrapException;
import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public abstract class PayaraMicroServer implements Runnable {

    protected static final Logger LOG = LoggerFactory.getLogger(PayaraMicroServer.class);

    private final Integer httpPort;

    private final CountDownLatch latch;

    protected PayaraMicroServer(Integer httpPort, CountDownLatch latch) {
        this.httpPort = httpPort;
        this.latch = latch;
    }

    @Override
    public void run() {
        PayaraMicro payaraMicro = createPayaraMicro();
        try {
            PayaraMicroRuntime runtime = payaraMicro.bootStrap();
            latch.await();
            try {
                runtime.shutdown();
            } catch (BootstrapException e) {
                LOG.warn("Error has occurred while shutdown payara-micro server.", e);
                throw new GradleException("Fail to shutdown payara-micro server.", e);
            }
        } catch (BootstrapException e) {
            LOG.error("Error has occurred while bootstrapping payara-micro server.", e);
            throw new GradleException("Fail to start payara-micro server.", e);
        } catch (InterruptedException e) {
            LOG.warn("Error has occurred while payara-micro server is running.", e);
        }
    }

    protected abstract PayaraMicro createPayaraMicro();

    public Integer getHttpPort() {
        return httpPort;
    }
}
