@echo off
setlocal

if exist "%MULE_HOME%" goto :continue
echo You must set the environment variable MULE_HOME to the location of your Mule install in order to run this example. 
goto :eof

:continue
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM This extends the classpath to include the configuration directory
REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf
SET ACTIVEMQ=activemq-core-4.1.2.jar

if exist "%MULE_BASE%\lib\user\%ACTIVEMQ%" goto :mule
if exist "%MULE_HOME%\lib\user\%ACTIVEMQ%" goto :mule
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
call "%MULE_BASE%\bin\mule.bat" -config error-config.xml
