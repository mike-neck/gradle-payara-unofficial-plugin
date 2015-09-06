Gradle Payara (Unofficial) Plugin
===

Gradle payara (unofficial) plugin enables you to run JavaEE app on payara-micro like `jetty` plugin or `gretty` plugin.
This plugin depends on `war` plugin so that `war` plugin will be applied to the project.

Tasks
===

* `payraRunWar` - builds war file, runs payara-micro server and deploys war file on payara-micro
* `parayaStop` - stop payara-micro server
* `payaraRun` - T.B.D. run payara micro

Apply Plugin
===

This plugin is delivered via [Gradle Plugins Portal](https://plugins.gradle.org/). In order to apply this plugin, add following script to your `build.gradle`.

##### New plugin mechanism

```groovy
plugins {
  id 'org.mikeneck.payara-plugin' version '0.0.2'
}
```
##### Old plugin mechanism

```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.org.mikeneck:payara-plugin:0.0.2"
  }
}
apply plugin: 'org.mikeneck.payara-plugin'
```

Settings
===

You can configure these values via `payara{}` block.

value|type|description
:--|:--:|:--
httpPort|`int`|a port number for payara-micro server to run(default 8080)
stopPort|`int`|a port number to stop payara-micro server(default 5050)
stopCommand|`String`|stop command to stop payara-micro server(default `"stop"`)
daemon|`boolean`|run payara-micro server as daemon mode(default `false`)

```groovy
payara {
  httpPort = 8000
  stopPort = 5000
  stopCommand = 'shutdown'
  daemon = true
  war = tasks.war.archivePath
}
```

Use case
===

1. run application on payara-micro server
1. run test
1. stop payara-micro server

```groovy
// enable daemon mode
payara {
  daemon = true
}
// before integrationTest start payara-micro server and deploy war
integrationTest.dependsOn payaraRunWar
// after integrationTest stop payara-micro server
integrationTest.finalizedBy payaraStop
```

Features
===

* deploy a war which is built from project
* run payara-micro server as daemon mode in order to run test

Currently unsupported
===

* **daemon mode is currently unavailable.**(i.e. `payaraRunWar` task will block the build and tests is not available in the same build.)
* deploy classes
* deploy multiple wars
* deploy war file
* run multiple servers with clustering them
* auto redeploy war

Customize task
===

You can create own task. Available task types are...

type|description
:--|:--
`RunPayara`|running paraya-micro server with war file
`StopPayara`|stopping payara-micro server

### `RunPayara` task configuration

##### Properties

property|type|description
:--|:--:|:--
`httpPort`|`int`|port number of payara-micro(default 8080)
`stopPort`|`int`|port number for stopping server(default 5050)
`stopCommand`|`String`|stop command for stopping server(default `"stop"`)
`daemon`|`boolean`|if `true`, run payara-micor server as daemon mode.
`warFile`|`File`|war file to be deployed

### `StopPayara` task configuration

##### Properties

property|type|description
:--|:--:|:--
`stopPort`|`int`|port number for stopping server(default 5050)
`stopCommand`|`String`|stop command for stopping server(default `"stop"`)
