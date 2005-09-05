@echo off

REM   Mule sethome shell script
REM
REM   $Id$
REM
REM
REM   Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
REM   reserved.

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_MULE_HOME=%~dp0..

if "%MULE_HOME%"=="" set MULE_HOME=%DEFAULT_MULE_HOME%
set DEFAULT_MULE_HOME=

:doneStart
rem find MULE_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%MULE_HOME%\README.txt" goto end

rem check for mule in Program Files on system drive
if not exist "%SystemDrive%\Program Files\mule" goto checkSystemDrive
set MULE_HOME=%SystemDrive%\Program Files\mule
goto end

:checkSystemDrive
rem check for mule in root directory of system drive
if not exist %SystemDrive%\mule\README.txt goto checkCDrive
set MULE_HOME=%SystemDrive%\mule
goto end

:checkCDrive
rem check for mule in C:\mule for Win9X users
if not exist C:\mule\README.txt goto noMuleHome
set MULE_HOME=C:\mule
goto end


:end
echo MULE_HOME is %MULE_HOME%
