@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..

REM Set your application specific classpath like this
SET CLASSPATH=%MULE_HOME%\samples\loanbroker-esb\conf;%MULE_HOME%\samples\loanbroker-esb\classes;
SET CUSTOM_LIB=%MULE_HOME%\samples\loanbroker-esb\lib
REM set the LoanConsumer class as the main class to run
SET MULE_MAIN=org.mule.samples.loanbroker.esb.Main

call %MULE_HOME%\bin\mule.bat

SET MULE_MAIN=
SET CLASSPATH=