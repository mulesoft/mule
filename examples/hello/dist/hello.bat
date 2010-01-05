@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\mule-example-hello" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
ECHO The Hello example is available in two variations:
ECHO   1. Hello from the Command Line
ECHO   2. Hello using the HTTP transport
ECHO      (surf to http://localhost:8888/?name=Dolly)
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-hello\hello-config.xml
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-hello\hello-http-config.xml
