@echo off

REM   Mule shell script
REM
REM   $Id$
REM
REM
REM   Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
REM   reserved.

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_MULE_HOME=%~dp0..

if "%MULE_HOME%"=="" set MULE_HOME=%DEFAULT_MULE_HOME%
set DEFAULT_MULE_HOME=

if exist "%MULE_HOME%\bin\mule_pre.bat" call "%MULE_HOME%\bin\mule_pre.bat"


rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set MULE_CMD_LINE_ARGS=%1
if ""%1""=="""" goto doneStart
shift
:setupArgs
if ""%1""=="""" goto doneStart
set MULE_CMD_LINE_ARGS=%MULE_CMD_LINE_ARGS% %1
shift
goto setupArgs
rem This label provides a place for the argument list loop to break out 
rem and for NT handling to skip to.

:doneStart
rem find MULE_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%MULE_HOME%\README.txt" goto checkJava

rem check for mule in Program Files on system drive
if not exist "%SystemDrive%\Program Files\mule" goto checkSystemDrive
set MULE_HOME=%SystemDrive%\Program Files\mule
goto checkJava

:checkSystemDrive
rem check for mule in root directory of system drive
if not exist %SystemDrive%\mule\README.txt goto checkCDrive
set MULE_HOME=%SystemDrive%\mule
goto checkJava

:checkCDrive
rem check for mule in C:\mule for Win9X users
if not exist C:\mule\README.txt goto noMuleHome
set MULE_HOME=C:\mule
goto checkJava

:noMuleHome
echo MULE_HOME is set incorrectly or mule could not be located. Please set MULE_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%
set LOCALCLASSPATH=%CLASSPATH%

set JAVA_EXT_DIRS=%JAVA_HOME%\lib\ext;%MULE_HOME%;%MULE_HOME%\lib;%MULE_HOME%\lib\optional

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto runMule

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo.

:runMule

if "%MULE_OPTS%" == "" set MULE_OPTS=-Xmx512M 

if "%MULE_MAIN%" == "" set MULE_MAIN=org.mule.MuleServer 

REM Uncomment to enable YourKit profiling
REM SET mule_DEBUG_OPTS="-Xrunyjpagent"

REM Uncomment to enable remote debugging
REM SET mule_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

set LOCALCLASSPATH=%MULE_HOME%\conf;%LOCALCLASSPATH%


"%_JAVACMD%" %mule_DEBUG_OPTS% %MULE_OPTS% -Djava.ext.dirs="%JAVA_EXT_DIRS%" -classpath "%LOCALCLASSPATH%" -Dmule.home="%MULE_HOME%" %MULE_MAIN% %MULE_ARGS% %MULE_CMD_LINE_ARGS%

goto end


:end
set LOCALCLASSPATH=
set _JAVACMD=
set MULE_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal

:mainEnd

if exist "%MULE_HOME%\bin\mule_post.bat" call "%MULE_HOME%\bin\mule_post.bat"

