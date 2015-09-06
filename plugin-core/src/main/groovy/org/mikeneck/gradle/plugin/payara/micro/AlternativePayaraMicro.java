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
package org.mikeneck.gradle.plugin.payara.micro;

import fish.payara.micro.BootstrapException;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.PortBinder;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.hazelcast.MulticastConfiguration;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFish.Status;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.LogManager;

import static com.sun.enterprise.glassfish.bootstrap.StaticGlassFishRuntime.copy;

public class AlternativePayaraMicro {

    private static final Logger logger = LoggerFactory.getLogger(AlternativePayaraMicro.class);

    private static final String CLUSTER_PORT = "embedded-glassfish-config.server.hazelcast-runtime-configuration.multicastPort";

    private static final String CLUSTER_MULTICAST_GROUP = "embedded-glassfish-config.server.hazelcast-runtime-configuration.multicastGroup";

    private static final String MAX_HTTP_THREADS = "embedded-glassfish-config.server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size";

    private static final String MIN_HTTP_THREADS = "embedded-glassfish-config.server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size";

    private static AlternativePayaraMicro instance;

    private String clusterMulticastGroup;
    private int clusterPort = Integer.MIN_VALUE;
    private int clusterStartPort = Integer.MIN_VALUE;
    private int httpPort = Integer.MIN_VALUE;
    private int sslPort = Integer.MIN_VALUE;
    private int maxHttpThreads = Integer.MIN_VALUE;
    private int minHttpThreads = Integer.MIN_VALUE;
    private String instanceName = UUID.randomUUID().toString();
    private File rootDir;
    private File deploymentDir;
    private File alternateDomainXML;
    private File alternateHZConfigFile;
    private List<File> deployments;
    private GlassFish gf;
    private PayaraMicroRuntime runtime;
    private boolean noCluster = false;
    private boolean autoBindHttp = false;
    private boolean autoBindSsl = false;
    private int autoBindRange = 5;

    public static AlternativePayaraMicro getInstance() {
        return getInstance(true);
    }

    public static AlternativePayaraMicro getInstance(boolean create) {
        if (instance == null && create) {
            instance = new AlternativePayaraMicro();
        }
        return instance;
    }

    public static PayaraMicroRuntime bootstrap() throws BootstrapException {
        return getInstance().bootStrap();
    }

    private AlternativePayaraMicro() {
        addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("GlassFish Shutdown Hook") {
            @Override
            public void run() {
                try {
                    if (gf != null) {
                        gf.stop();
                        gf.dispose();
                    }
                } catch (GlassFishException ignore) {
                }
            }
        });
    }

    private void verifyPayaraMicroIsNotRunning() throws IllegalStateException {
        if (runtime != null) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect.");
        }
    }

    public String getClusterMulticastGroup() {
        return clusterMulticastGroup;
    }

    public AlternativePayaraMicro setClusterMulticastGroup(String hzMulticastGroup) {
        verifyPayaraMicroIsNotRunning();
        this.clusterMulticastGroup = hzMulticastGroup;
        return this;
    }

    public int getClusterPort() {
        return clusterPort;
    }

    public AlternativePayaraMicro setClusterPort(int hzPort) {
        verifyPayaraMicroIsNotRunning();
        this.clusterPort = hzPort;
        return this;
    }

    public int getClusterStartPort() {
        return clusterStartPort;
    }

    public AlternativePayaraMicro setClusterStartPort(int clusterStartPort) {
        verifyPayaraMicroIsNotRunning();
        this.clusterStartPort = clusterStartPort;
        return this;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public AlternativePayaraMicro setHttpPort(int httpPort) {
        verifyPayaraMicroIsNotRunning();
        this.httpPort = httpPort;
        return this;
    }

    public int getSslPort() {
        return sslPort;
    }

    public AlternativePayaraMicro setSslPort(int sslPort) {
        verifyPayaraMicroIsNotRunning();
        this.sslPort = sslPort;
        return this;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public AlternativePayaraMicro setInstanceName(String instanceName) {
        verifyPayaraMicroIsNotRunning();
        this.instanceName = instanceName;
        return this;
    }

    public File getDeploymentDir() {
        return deploymentDir;
    }

    public AlternativePayaraMicro setDeploymentDir(File deploymentDir) {
        verifyPayaraMicroIsNotRunning();
        this.deploymentDir = deploymentDir;
        return this;
    }

    public File getAlternateDomainXML() {
        return alternateDomainXML;
    }

    public AlternativePayaraMicro setAlternateDomainXML(File alternateDomainXML) {
        verifyPayaraMicroIsNotRunning();
        this.alternateDomainXML = alternateDomainXML;
        return this;
    }

    public AlternativePayaraMicro addDeployment(String pathToWar) {
        verifyPayaraMicroIsNotRunning();
        File file = new File(pathToWar);
        return addDeploymentFile(file);
    }

    public boolean isNoCluster() {
        return noCluster;
    }

    public AlternativePayaraMicro setNoCluster(boolean noCluster) {
        verifyPayaraMicroIsNotRunning();
        this.noCluster = noCluster;
        return this;
    }

    public AlternativePayaraMicro addDeploymentFile(File file) {
        verifyPayaraMicroIsNotRunning();
        if (deployments == null) {
            deployments = new LinkedList<>();
        }
        deployments.add(file);
        return this;
    }

    public int getMaxHttpThreads() {
        return maxHttpThreads;
    }

    public AlternativePayaraMicro setMaxHttpThreads(int maxHttpThreads) {
        verifyPayaraMicroIsNotRunning();
        this.maxHttpThreads = maxHttpThreads;
        return this;
    }

    public int getMinHttpThreads() {
        return minHttpThreads;
    }

    public AlternativePayaraMicro setMinHttpThreads(int minHttpThreads) {
        verifyPayaraMicroIsNotRunning();
        this.minHttpThreads = minHttpThreads;
        return this;
    }

    public File getRootDir() {
        return rootDir;
    }

    public AlternativePayaraMicro setRootDir(File rootDir) {
        verifyPayaraMicroIsNotRunning();
        this.rootDir = rootDir;
        return this;
    }

    public boolean isAutoBindHttp() {
        return autoBindHttp;
    }

    public void setAutoBindHttp(boolean autoBindHttp) {
        this.autoBindHttp = autoBindHttp;
    }

    public boolean isAutoBindSsl() {
        return autoBindSsl;
    }

    public void setAutoBindSsl(boolean autoBindSsl) {
        this.autoBindSsl = autoBindSsl;
    }

    public int getAutoBindRange() {
        return autoBindRange;
    }

    public void setAutoBindRange(int autoBindRange) {
        this.autoBindRange = autoBindRange;
    }

    public PayaraMicroRuntime bootStrap() throws BootstrapException {
        if (runtime != null) {
            throw new IllegalStateException("Payara Micro is already running, calling bootstrap now is meaningless.");
        }
        if (!noCluster) {
            MulticastConfiguration mc = new MulticastConfiguration();
            mc.setMemberName(instanceName);
            if(clusterPort > Integer.MIN_VALUE) {
                mc.setMulticastPort(clusterPort);
            }
            if(clusterStartPort > Integer.MIN_VALUE) {
                mc.setStartPort(clusterStartPort);
            }
            if(clusterMulticastGroup != null) {
                mc.setMulticastGroup(clusterMulticastGroup);
            }
            if(alternateHZConfigFile != null) {
                mc.setAlternateConfiguration(alternateHZConfigFile);
            }
            HazelcastCore.setMulticastOverride(mc);
        }

        setSystemProperties();
        BootstrapProperties bpr = new BootstrapProperties();
        GlassFishRuntime gfRuntime;
        PortBinder binder = new PortBinder();

        try {
            gfRuntime = GlassFishRuntime.bootstrap(bpr, Thread.currentThread().getContextClassLoader());
            GlassFishProperties gfProp = new GlassFishProperties();
            // bind http port
            if (httpPort != Integer.MIN_VALUE) {
                if (autoBindHttp) {
                    bindHttp().listenerTo(gfProp).port(httpPort).using(binder);
                } else {
                    gfProp.setPort("http-listener", httpPort);
                }
            } else {
                if (autoBindHttp) {
                    bindHttp().listenerTo(gfProp).port(8080).using(binder);
                }
            }
            // bind https port
            if (sslPort != Integer.MIN_VALUE) {
                if (autoBindSsl) {
                    bindHttps().listenerTo(gfProp).port(sslPort).using(binder);
                } else {
                    gfProp.setPort("https-lisetener", sslPort);
                }
            }
            // provide domain xml
            if (alternateDomainXML != null) {
                gfProp.setConfigFileReadOnly(false);
                gfProp.setConfigFileURI("file:///" + alternateDomainXML.getAbsolutePath().replace('\\', '/'));
            } else {
                String clusterXml = noCluster ? "microdomain-nocluseter.xml" : "microdomain.xml";
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                URL url = loader.getResource(clusterXml);
                String xtf = url.toExternalForm();
                gfProp.setConfigFileReadOnly(false);
                gfProp.setConfigFileURI(xtf);
            }

            // load domain xml
            if (rootDir != null) {
                gfProp.setInstanceRoot(rootDir.getAbsolutePath());
                File confFile = new File(rootDir.getAbsolutePath() + File.separator + "config" + File.separator + "domain.xml");
                if (!confFile.exists()) {
                    installFiles(gfProp);
                } else {
                    String path = rootDir.getAbsolutePath().replace('\\', '/');
                    gfProp.setConfigFileReadOnly(false);
                    gfProp.setConfigFileURI("file:///" + path + "/config/domain.xml");
                }
            }

            // set cluster port
            if (clusterPort != Integer.MIN_VALUE) {
                gfProp.setProperty(CLUSTER_PORT, Integer.toString(clusterPort));
            }
            // set cluster group
            if (clusterMulticastGroup != null) {
                gfProp.setProperty(CLUSTER_MULTICAST_GROUP, clusterMulticastGroup);
            }
            // set min http threads
            if (minHttpThreads != Integer.MIN_VALUE) {
                gfProp.setProperty(MIN_HTTP_THREADS, Integer.toString(minHttpThreads));
            }
            // set max http threads
            if (maxHttpThreads != Integer.MIN_VALUE) {
                gfProp.setProperty(MAX_HTTP_THREADS, Integer.toString(maxHttpThreads));
            }

            gf = gfRuntime.newGlassFish(gfProp);

            // reset log manager
            File confDir = new File(System.getProperty("com.sun.aas.instanceRoot"), "config");
            File logProp = new File(confDir.getAbsolutePath(), "logging.properties");
            if (logProp.exists() && logProp.canRead() && logProp.isFile()) {
                System.setProperty("java.util.logging.config.file", logProp.getAbsolutePath());
                try {
                    LogManager.getLogManager().readConfiguration();
                } catch (SecurityException | IOException e) {
                    logger.error("Payara micro logger is not available", e);
                }
            }
            // run glassfish
            gf.start();
            try {
                Constructor<PayaraMicroRuntime> constructor = PayaraMicroRuntime.class.getDeclaredConstructor(String.class, GlassFish.class);
                constructor.setAccessible(true);
                runtime = constructor.newInstance(instanceName, gf);
                deployAll();
                return runtime;
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                logger.error("Cannot create PayaraMicroRuntime.", e);
                throw new GlassFishException("Cannot create PayaraMicroRuntime", e);
            }
        } catch (GlassFishException e) {
            throw new BootstrapException(e.getMessage(), e);
        }
    }

    private BindingSubject bindHttp() {
        return bind("http");
    }

    private BindingSubject bindHttps() {
        return bind("https");
    }

    private BindingSubject bind(String protocol) {
        return new BindingSubject(protocol) {
            @Override
            PortAround listenerTo(final GlassFishProperties gfProp) {
                return new PortAround() {
                    @Override
                    public PortBinding port(final int port) {
                        return new PortBinding() {
                            @Override
                            public void using(final PortBinder binder) throws GlassFishException {
                                try {
                                    gfProp.setPort(listener, binder.findAvailablePort(port, autoBindRange));
                                } catch (BindException e) {
                                    String msg = String.format("No available port for %s, in range %d - %d.", protocol, port, autoBindRange);
                                    logger.error(msg, e);
                                    throw new GlassFishException("Could not bind HTTP port.", e);
                                }
                            }
                        };
                    }
                };
            }
        };
    }

    private interface PortBinding {
        void using(PortBinder binder) throws GlassFishException;
    }

    private interface PortAround {
        PortBinding port(int port);
    }

    private abstract class BindingSubject {
        protected final String listener;
        protected final String protocol;
        BindingSubject(String protocol) {
            this.protocol = protocol;
            this.listener = protocol + "-listener";
        }
        abstract PortAround listenerTo(GlassFishProperties gfProp);
    }

    public PayaraMicroRuntime getRuntime() throws IllegalStateException {
        if (!isRunning()) {
            throw new IllegalStateException("Payara Micro is not running.");
        }
        return runtime;
    }

    public void shutdown() throws IllegalStateException, BootstrapException {
        if (!isRunning()) {
            throw new IllegalStateException("Payara Micro is not running.");
        }
        runtime.shutdown();
    }

    private void deployAll() throws GlassFishException {
        int depCount = 0;
        Deployer deployer = gf.getDeployer();
        // deploy explicit wars
        if (deployments != null) {
            for (File war : deployments) {
                if (war.exists() && war.isFile() && war.canRead()) {
                    deployer.deploy(war, "--availabilityenabled=true");
                    depCount++;
                } else {
                    logger.info("{} is not a valid deployment", war.getAbsolutePath());
                }
            }
        }
        // deploy from deployment director
        if (deploymentDir != null) {
            for (File war : deploymentDir.listFiles()) {
                String archive = war.getAbsolutePath();
                if (war.isFile() && war.canRead() &&
                        (archive.endsWith(".war") || archive.endsWith(".ear") ||
                                archive.endsWith(".jar") || archive.endsWith(".rar"))) {
                    deployer.deploy(war, "--availabilityenabled=true");
                    depCount++;
                }
            }
        }
        logger.info("Deployed {} wars", depCount);
    }

    private void installFiles(GlassFishProperties gfProp) {
        File confDir = new File(rootDir.getAbsolutePath(), "config");
        new File(rootDir.getAbsolutePath(), "docroot").mkdirs();
        confDir.mkdirs();
        String confDirPath = confDir.getAbsolutePath();

        String[] configFiles = new String[]{"config/keyfile",
                "config/server.policy",
                "config/cacerts.jks",
                "config/keystore.jks",
                "config/login.conf",
                "config/logging.properties",
                "config/admin-keyfile",
                "config/default-web.xml",
                "org/glassfish/embed/domain.xml"
        };
        ClassLoader loader = getClass().getClassLoader();
        for (String configFile : configFiles) {
            URL url = loader.getResource(configFile);
            if (url != null) {
                copy(url, new File(confDirPath,
                                configFile.substring(configFile.lastIndexOf('/') + 1)), false);
            }
        }

        URL brandingUrl = loader.getResource("config/branding/glassfish-version.properties");
        if (brandingUrl != null) {
            copy(brandingUrl, new File(confDirPath, "branding/glassfish-version.properties"), false);
        }

        String uri = gfProp.getConfigFileURI();
        try {
            copy(URI.create(uri).toURL(), new File(confDirPath, "domain.xml"), true);
        } catch (MalformedURLException e) {
            logger.warn("domain.xml is not available.", e);
        }
    }

    private void setSystemProperties() {
        try {
            Properties prop = new Properties();
            prop.load(getClass().getClassLoader().getResourceAsStream("payara-boot.properties"));
            for (Object o : prop.keySet()) {
                String key = (String) o;
                if (System.getProperty(key) == null) {
                    System.setProperty(key, prop.getProperty(key));
                }
            }
        } catch (IOException e) {
            logger.warn("payara-boot.properties file is not found.", e);
        }
    }

    boolean isRunning() {
        try {
            return gf != null && gf.getStatus() == Status.STARTED;
        } catch (GlassFishException e) {
            return false;
        }
    }
}
