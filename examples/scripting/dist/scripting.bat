@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\scripting-groovy" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
ECHO The Scripting example is available in two variations:
ECHO   1. Groovy
ECHO   2. Python
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -app scripting-groovy
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -app scripting-python
