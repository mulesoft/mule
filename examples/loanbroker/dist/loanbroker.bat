@echo off
setlocal
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

REM 3rd party libraries for LoanBroker ESB
SET ACTIVEMQ=activemq-core-4.1.0-incubator.jar
SET OPENEJB=openejb-core-1.0.jar
REM 3rd party libraries for LoanBroker BPM
SET BSH=bsh-1.3.0.jar
SET HSQLDB=hsqldb-1.8.0.7.jar

:testactivemq
if exist "%MULE_BASE%\lib\user\%ACTIVEMQ%" goto :testopenejb
if exist "%MULE_HOME%\lib\user\%ACTIVEMQ%" goto :testopenejb
goto :missinglibs

:testopenejb
if exist "%MULE_BASE%\lib\user\%OPENEJB%" goto :testbsh
if exist "%MULE_HOME%\lib\user\%OPENEJB%" goto :testbsh
goto :missinglibs

:testbsh
if exist "%MULE_BASE%\lib\user\%BSH%" goto :testhsqldb
if exist "%MULE_HOME%\lib\user\%BSH%" goto :testhsqldb
goto :missinglibs

:testhsqldb
if exist "%MULE_BASE%\lib\user\%HSQLDB%" goto :mule
if exist "%MULE_HOME%\lib\user\%HSQLDB%" goto :mule

:missinglibs
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
ECHO The Loan Broker example is available in three flavors:
ECHO   1. Loan Broker ESB
ECHO   2. Loan Broker ESN
ECHO   3. Loan Broker BPM
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -main org.mule.examples.loanbroker.esb.LoanBrokerApp
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -main org.mule.examples.loanbroker.esn.LoanBrokerApp
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -main org.mule.examples.loanbroker.bpm.LoanBrokerApp

