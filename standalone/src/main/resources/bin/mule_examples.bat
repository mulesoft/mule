@echo off
setlocal

rem ###############################################################
rem Deploy examples and launch Mule ESB
rem ###############################################################

set URL=http://localhost:18082/examples/

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt

rem ###############################################################
rem Setting MULE_HOME if the script is called from outside mule.bat script
rem ###############################################################
if "%MULE_HOME%" == "" set MULE_HOME=%_REALPATH:~0,-5%

rem ###############################################################
rem Deploy examples and launch Mule ESB
rem ###############################################################

call %MULE_HOME%\bin\launcher.bat %MULE_HOME%\bin\mule_examples.groovy "%URL%"

if not ERRORLEVEL 1 goto 
echo "ERROR: %ERRORLEVEL%. Please check log file (%MULE_HOME%\logs\mule.log) to see why Mule ESB is not starting"
goto :eof

:open_url
Start %URL%
goto :eof

:eof
