@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

if exist "%MULE_BASE%\lib\user\activemq.jar" goto :mule
if exist "%MULE_HOME%\lib\user\activemq.jar" goto :mule
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
call %MULE_BASE%\bin\mule.bat -config .\conf\jms-config.xml
