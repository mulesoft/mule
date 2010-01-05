@echo off

rem dynamically evaluate the name of the groovy jar
for /F %%v in ('dir /b lib\opt^| findstr groovy') do set GROOVY_JAR=%%v
set GROOVY_PATH=%MULE_HOME%\lib\opt\%GROOVY_JAR%

rem dynamically evaluate the name of the commons-cli jar
for /F %%v in ('dir /b lib\boot^| findstr commons-cli') do set COMMONS_CLI_JAR=%%v
set COMMONS_CLI_PATH=%MULE_HOME%\lib\boot\%COMMONS_CLI_JAR%

set cp=.;%MULE_HOME%\conf
set cp=%cp%;%GROOVY_PATH%;%COMMONS_CLI_PATH%
java -Dmule.home=%MULE_HOME% -cp "%cp%" org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain --conf %MULE_HOME%\bin\launcher.conf %*
