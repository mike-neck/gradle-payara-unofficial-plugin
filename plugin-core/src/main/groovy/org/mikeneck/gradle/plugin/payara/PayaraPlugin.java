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
package org.mikeneck.gradle.plugin.payara;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.bundling.War;
import org.mikeneck.gradle.plugin.payara.model.PayaraSetting;
import org.mikeneck.gradle.plugin.payara.model.PayaraSettingPojo;
import org.mikeneck.gradle.plugin.payara.task.AbstractPayaraTask;
import org.mikeneck.gradle.plugin.payara.task.PayaraRunWar;
import org.mikeneck.gradle.plugin.payara.task.PayaraStop;

import java.util.concurrent.Callable;

public class PayaraPlugin implements Plugin<Project> {

    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int DEFAULT_STOP_PORT = 5050;

    public static final String DEFAULT_STOP_COMMAND = "stop";

    public static final boolean DEFAULT_DAEMON = false;

    public static final String PAYARA_CONVENTION = "payara";

    public static final String STOP_METHOD = "POST";

    public static final String HTTP_PORT = "httpPort";

    public static final String STOP_PORT = "stopPort";

    public static final String STOP_COMMAND = "stopCommand";

    public static final String DAEMON = "daemon";

    /**
     * minimum port number(included)
     */
    public static final int MIN_PORT_NUMBER = 1024;

    /**
     * maximum port number(included)
     */
    public static final int MAX_PORT_NUMBER = 49151;

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(WarPlugin.class);
        PayaraSetting setting = new PayaraSettingPojo(DEFAULT_HTTP_PORT, DEFAULT_STOP_PORT, DEFAULT_STOP_COMMAND, DEFAULT_DAEMON);
        Convention convention = project.getConvention();
        convention.getPlugins().put(PAYARA_CONVENTION, setting);

        configureMappingRules(project, setting);
        configurePayaraRunWar(project, setting);
        configurePayaraStop(project, setting);
    }

    private static void configureMappingRules(final Project project, final PayaraSetting setting) {
        project.getTasks().withType(AbstractPayaraTask.class, new Action<AbstractPayaraTask>() {
            @Override
            public void execute(AbstractPayaraTask task) {
                configurePayara(setting, task);
            }
        });
    }

    private static void configurePayara(final PayaraSetting setting, final AbstractPayaraTask task) {
        task.getConventionMapping().map(HTTP_PORT, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return setting.getHttpPort();
            }
        });
        task.getConventionMapping().map(STOP_PORT, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return setting.getStopPort();
            }
        });
        task.getConventionMapping().map(STOP_COMMAND, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return setting.getStopCommand();
            }
        });
        task.getConventionMapping().map(DAEMON, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return setting.getDaemon();
            }
        });
    }

    private static void configurePayaraRunWar(final Project project, final PayaraSetting setting) {
        project.getTasks().withType(PayaraRunWar.class, new Action<PayaraRunWar>() {
            @Override
            public void execute(PayaraRunWar task) {
                task.dependsOn(WarPlugin.WAR_TASK_NAME);
                task.getConventionMapping().map("war", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return ((War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME)).getArchivePath();
                    }
                });
            }
        });
        PayaraRunWar task = project.getTasks().create(PayaraRunWar.TASK_NAME, PayaraRunWar.class);
        task.setGroup(WarPlugin.WEB_APP_GROUP);
        task.setDescription(PayaraRunWar.DESCRIPTION);
    }

    private static void configurePayaraStop(final Project project, final PayaraSetting setting) {
        PayaraStop task = project.getTasks().create(PayaraStop.TASK_NAME, PayaraStop.class);
        task.getConventionMapping().map(STOP_PORT, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return setting.getStopPort();
            }
        });
        task.getConventionMapping().map(STOP_COMMAND, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return setting.getStopCommand();
            }
        });
        task.setGroup(WarPlugin.WEB_APP_GROUP);
        task.setDescription(PayaraStop.DESCRIPTION);
    }
}
