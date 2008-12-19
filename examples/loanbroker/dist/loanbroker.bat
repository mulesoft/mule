@echo off
setlocal

if exist "%MULE_HOME%" goto :continue
echo You must set the environment variable MULE_HOME to the location of your Mule install in order to run this example. 
goto :eof

:continue
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Translate MULE_HOME to unix notation. That's used to create a system property
REM mule.home.unix below which is used to configure openejb
set MULE_HOME_UNIX="%MULE_HOME:\=/%"

REM This extends the classpath to include the configuration directory
REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

REM 3rd party libraries for LoanBroker ESB
SET ACTIVEMQ=activemq-core-4.1.2.jar
SET OPENEJB=openejb-core-1.0.jar
REM 3rd party libraries for LoanBroker BPM
SET BSH=bsh-1.3.0.jar
SET DERBYDB=derby-10.3.1.4.jar

:testactivemq
if exist "%MULE_BASE%\lib\user\%ACTIVEMQ%" goto :testopenejb
if exist "%MULE_HOME%\lib\user\%ACTIVEMQ%" goto :testopenejb
goto :missinglibs

:testopenejb
if exist "%MULE_BASE%\lib\user\%OPENEJB%" goto :testbsh
if exist "%MULE_HOME%\lib\user\%OPENEJB%" goto :testbsh
goto :missinglibs

:testbsh
if exist "%MULE_BASE%\lib\user\%BSH%" goto :testderbydb
if exist "%MULE_HOME%\lib\user\%BSH%" goto :testderbydb
goto :missinglibs

:testderbydb
if exist "%MULE_BASE%\lib\user\%DERBYDB%" goto :mule
if exist "%MULE_HOME%\lib\user\%DERBYDB%" goto :mule

:missinglibs
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
ECHO The Loan Broker example is available in three flavors:
ECHO   1. Loan Broker ESB
ECHO   2. Loan Broker ESN
ECHO   3. Loan Broker BPM
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -main org.mule.example.loanbroker.esb.LoanBrokerApp -M-Dmule.home.unix="%MULE_HOME_UNIX%"
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -main org.mule.example.loanbroker.esn.LoanBrokerApp
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -main org.mule.example.loanbroker.bpm.LoanBrokerApp
