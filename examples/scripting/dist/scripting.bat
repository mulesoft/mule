@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\mule-example-scripting" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
ECHO The Scripting example is available in two flavors:
ECHO   1. Groovy
ECHO   2. Python
SET /P Choice=Select the one you wish to execute and press Enter...
IF '%Choice%'=='1' SET SCRIPTFILE="greedy.groovy"
IF '%Choice%'=='2' SET SCRIPTFILE="greedy.py"
if "%SCRIPTFILE%" == "" goto :run

ECHO Welcome to the JSR 223-powered Change Machine.  This machine will give you
ECHO change for the amount you request.  The amount of change is cumulative, so you
ECHO can keep asking for more and more change until you've had enough.
ECHO To get started, select a currency:
ECHO   1. U.S. Dollars
ECHO   2. U.K. Pounds Sterling
SET /P Choice=
IF '%Choice%'=='1' SET CURRENCY="USD"
IF '%Choice%'=='2' SET CURRENCY="GBP"
if "%CURRENCY%" == "" goto :run

call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-scripting\change-machine.xml -M-Dscriptfile=%SCRIPTFILE% -M-Dcurrency=%CURRENCY%
