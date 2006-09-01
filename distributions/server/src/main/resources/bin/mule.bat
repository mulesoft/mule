@echo off
setlocal

rem Copyright (c) 1999, 2006 Tanuki Software Inc.
rem
rem Java Service Wrapper general startup script
rem

rem
rem Resolve the real path of the wrapper.exe
rem  For non NT systems, the _REALPATH and _WRAPPER_CONF values
rem  can be hard-coded below and the following test removed.
rem
if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0..\sbin\

rem Decide on the wrapper binary.
set _WRAPPER_BASE=wrapper
set _WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-x86-32.exe
if exist %_WRAPPER_EXE% goto conf
set _WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-x86-64.exe
if exist %_WRAPPER_EXE% goto conf
set _WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%.exe
if exist %_WRAPPER_EXE% goto conf
echo Unable to locate a Wrapper executable using any of the following names:
echo %_REALPATH%%_WRAPPER_BASE%-windows-x86-32.exe
echo %_REALPATH%%_WRAPPER_BASE%-windows-x86-64.exe
echo %_REALPATH%%_WRAPPER_BASE%.exe
pause
goto :eof

rem
rem Find the wrapper.conf
rem
:conf
rem set _WRAPPER_CONF="%~f1"
rem if not %_WRAPPER_CONF%=="" goto startup
set _WRAPPER_CONF="%_REALPATH%..\conf\wrapper.conf"

if not "%1"=="" set MULE_ARG1="%1"
if not "%2"=="" set MULE_ARG2="%2"
if not "%3"=="" set MULE_ARG3="%3"
if not "%4"=="" set MULE_ARG4="%4"
if not "%5"=="" set MULE_ARG5="%5"
if not "%6"=="" set MULE_ARG6="%6"
if not "%7"=="" set MULE_ARG7="%7"
if not "%8"=="" set MULE_ARG8="%8"
if not "%9"=="" set MULE_ARG9="%9"
if not "%10"=="" set MULE_ARG10="%10"


rem
rem Start the Wrapper
rem
:startup
"%_WRAPPER_EXE%" -c %_WRAPPER_CONF%
if not errorlevel 1 goto :eof
pause

