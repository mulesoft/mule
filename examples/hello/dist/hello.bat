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

if exist "%MULE_BASE%\apps\hello\hello-http-config.xml" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
REM TODO stdio may have lifecycle issues currently, disable it 
REM ECHO The Hello example is available in two variations:
REM ECHO   1. Hello from the Command Line
REM ECHO   2. Hello using the HTTP transport
REM ECHO      (surf to http://localhost:8888/?name=Dolly)
REM SET /P Choice=Select the one you wish to execute and press Enter...

REM IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config hello-config.xml
REM IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config hello-http-config.xml

call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\hello\hello-http-config.xml
