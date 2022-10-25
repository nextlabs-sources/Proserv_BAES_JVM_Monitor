TITLE NextLabs JVM Monitoring Tool

set HOME_DIR=%~dp0/../

set LIB_HOME=%HOME_DIR%/xlib

set PR_JVMOPTIONS=-Dconfig.file.path="%HOME_DIR%/config/"

set PR_CLASSPATH="%HOME_DIR%/config/;%LIB_HOME%/*;%HOME_DIR%/Nextlabs-JVMMonitoring.jar"

set START_CLASS=com.nextlabs.monitoring.Monitor

"java" %PR_JVMOPTIONS% -cp %PR_CLASSPATH% -server -Xms128M -Xmx256M %START_CLASS%