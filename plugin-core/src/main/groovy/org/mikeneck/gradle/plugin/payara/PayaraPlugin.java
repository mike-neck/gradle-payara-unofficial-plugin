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
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.War;
import org.mikeneck.gradle.plugin.payara.model.PayaraSetting;
import org.mikeneck.gradle.plugin.payara.model.PayaraSettingPojo;
import org.mikeneck.gradle.plugin.payara.task.PayaraRunWar;
import org.mikeneck.gradle.plugin.payara.task.PayaraStop;

import java.io.File;

public class PayaraPlugin implements Plugin<Project> {

    public static final int DEFAULT_HTTP_PORT = 8080;

    public static final int DEFAULT_STOP_PORT = 5050;

    public static final String DEFAULT_STOP_COMMAND = "stop";

    public static final boolean DEFAULT_DAEMON = false;

    public static final String PAYARA_CONVENTION = "payara";

    public static final String STOP_METHOD = "POST";

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
        final PayaraSetting setting = project.getExtensions()
                .create(PAYARA_CONVENTION,
                        PayaraSettingPojo.class,
                        DEFAULT_HTTP_PORT,
                        DEFAULT_STOP_PORT,
                        DEFAULT_STOP_COMMAND,
                        DEFAULT_DAEMON);

        final PayaraRunWar runWar = createPayaraRunWarTask(project.getTasks());
        final PayaraStop stopTask = createPayaraStopTask(project.getTasks());
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project prj) {
                configurePayaraRunWar(prj, runWar, setting);
                configurePayaraStop(stopTask, setting);
            }
        });
    }

    private static PayaraRunWar createPayaraRunWarTask(TaskContainer tasks) {
        PayaraRunWar task = tasks.create(PayaraRunWar.TASK_NAME, PayaraRunWar.class);
        task.setGroup(WarPlugin.WEB_APP_GROUP);
        task.dependsOn(WarPlugin.WAR_TASK_NAME);
        task.setDescription(PayaraRunWar.DESCRIPTION);
        return task;
    }

    private static PayaraStop createPayaraStopTask(TaskContainer tasks) {
        PayaraStop task = tasks.create(PayaraStop.TASK_NAME, PayaraStop.class);
        task.setGroup(WarPlugin.WEB_APP_GROUP);
        task.setDescription(PayaraStop.DESCRIPTION);
        return task;
    }

    private void configurePayaraStop(PayaraStop stopTask, PayaraSetting setting) {
        stopTask.setStopPort(setting.getStopPort());
        stopTask.setStopCommand(setting.getStopCommand());
    }

    private void configurePayaraRunWar(Project prj, PayaraRunWar runWar, PayaraSetting setting) {
        runWar.setHttpPort(setting.getHttpPort());
        runWar.setStopPort(setting.getStopPort());
        runWar.setStopCommand(setting.getStopCommand());
        runWar.setDaemon(setting.getDaemon());
        File archivePath = ((War) prj.getTasks().getByName(WarPlugin.WAR_TASK_NAME)).getArchivePath();
        runWar.setWar(archivePath);
    }
}
