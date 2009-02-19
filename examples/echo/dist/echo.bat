@echo off
setlocal
REM There is no need to call this if you set the MULE_HOME in your environment properties
REM but you must also define MULE_LIB for the example (see below)
REM or specify the config as a file: URI (see README.txt)
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM This extends the classpath to include the configuration directory
REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

ECHO The Echo example is available in three variations:
ECHO   1. Echo from the Command Line
ECHO   2. Echo using the Axis Transport
ECHO      (surf to http://localhost:65081/services/EchoUMO?method=echo^&param=hello)
ECHO   3. Echo using the Cxf Transport
ECHO      (surf to http://localhost:65082/services/EchoUMO/echo/text/hello)
SET /P Choice=Select the one you wish to execute and press Enter... 

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config .\conf\echo-config.xml
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config .\conf\echo-axis-config.xml
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -config .\conf\echo-cxf-config.xml
