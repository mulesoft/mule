@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\mule-example-echo" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
ECHO The Echo example is available in three variations:
ECHO   1. Echo from the Command Line
ECHO   2. Echo using the Axis Transport
ECHO      (surf to http://localhost:65081/services/EchoUMO?method=echo^&param=hello)
ECHO   3. Echo using the Cxf Transport
ECHO      (surf to http://localhost:65082/services/EchoUMO/echo/text/hello)
SET /P Choice=Select the one you wish to execute and press Enter... 

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-echo\echo-config.xml
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-echo\echo-axis-config.xml
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-echo\echo-cxf-config.xml
