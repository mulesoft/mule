@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_HOME%" goto :continue
echo You must set the environment variable MULE_HOME to the location of your Mule install in order to run this example.
goto :eof

:continue
java -Djava.ext.dirs=%MULE_BASE%\lib\shared\loanbroker;%MULE_BASE%\lib\boot;%MULE_BASE%\lib\mule;%MULE_BASE%\lib\opt org.mule.example.loanbroker.LoanBrokerApp
