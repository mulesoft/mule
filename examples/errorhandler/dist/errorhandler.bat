@echo off
setlocal
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf
SET ACTIVEMQ=activemq-core-4.1.0-incubator.jar

if exist "%MULE_BASE%\lib\user\%ACTIVEMQ%" goto :mule
if exist "%MULE_HOME%\lib\user\%ACTIVEMQ%" goto :mule
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
call "%MULE_BASE%\bin\mule.bat" -config .\conf\error-config.xml
