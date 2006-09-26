@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

if exist "%MULE_HOME%\lib\user\groovy.jar" goto :mule
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
ECHO The Scripting example is available in two variations:
ECHO   1. Binary HTTP
ECHO   2. Text file
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call %MULE_HOME%\bin\mule.bat -main org.mule.samples.scripting.BinaryHttpExample
IF '%Choice%'=='2' call %MULE_HOME%\bin\mule.bat -main org.mule.samples.scripting.TextFileExample
