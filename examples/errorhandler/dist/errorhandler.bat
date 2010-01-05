@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\mule-example-errorhandler" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
call "%MULE_BASE%\bin\mule.bat" -config %MULE_BASE%\apps\mule-example-errorhandler\error-config.xml
