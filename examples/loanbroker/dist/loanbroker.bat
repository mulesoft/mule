@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\mule-example-loanbroker" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
REM Translate MULE_HOME to unix notation. That's used to create a system property
REM mule.home.unix below which is used to configure openejb
set MULE_HOME_UNIX="%MULE_HOME:\=/%"

ECHO The Loan Broker example is available in three flavors:
ECHO   1. Loan Broker ESB
ECHO   2. Loan Broker ESN
ECHO   3. Loan Broker BPM
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -main org.mule.example.loanbroker.esb.LoanBrokerApp -M-Dmule.home.unix="%MULE_HOME_UNIX%"
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -main org.mule.example.loanbroker.esn.LoanBrokerApp
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -main org.mule.example.loanbroker.bpm.LoanBrokerApp
