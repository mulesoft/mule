@echo off
REM   Mule shell script
REM
REM   $Id$
REM
REM  Copyright 2001,2004 The Apache Software Foundation
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRMULEIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

if exist "%HOME%\mulerc_pre.bat" call "%HOME%\mulerc_pre.bat"

if "%OS%"=="Windows_NT" @setlocal

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

if exist %CD%\sethome.bat call %CD%\sethome.bat

if not exist %CD%\sethome.bat if %MULE_HOME% == "" echo Did not find sethome.bat in current directory and MULE_HOME is not set.

if %MULE_HOME% == "" goto :noMuleHome
goto checkJava

:noMuleHome
echo MULE_HOME is set incorrectly or mule could not be located. Please set MULE_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto checkJikes

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe

:checkJikes
if not "%JIKESPATH%"=="" goto runMuleWithJikes

:runMule

if "%MULE_OPTS%" == "" set MULE_OPTS=-Xmx512M 

if "%MULE_MAIN%" == "" set MULE_MAIN=org.mule.MuleServer

REM Uncomment to enable YourKit profiling
REM SET MULE_DEBUG_OPTS="-Xrunyjpagent"

REM Uncomment to enable remote debugging
REM SET MULE_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005


if not "%CLASSPATH%"=="" goto runMuleWithClasspath
"%_JAVACMD%" %MULE_DEBUG_OPTS% %MULE_OPTS% -classpath "%MULE_HOME%\bin\mule-launcher.jar" "-Dorg.mule.home=%MULE_HOME%" -Dorg.mule.main.class=%MULE_MAIN% org.mule.tools.launcher.Launcher %MULE_ARGS% %MULE_CMD_LINE_ARGS%
goto end

:runMuleWithClasspath
"%_JAVACMD%" %MULE_DEBUG_OPTS% %MULE_OPTS% -classpath "%MULE_HOME%\bin\mule-launcher.jar" "-Dorg.mule.home=%MULE_HOME%" -Dorg.mule.main.class=%MULE_MAIN% org.mule.tools.launcher.Launcher %MULE_ARGS% -lib "%CLASSPATH%" %MULE_CMD_LINE_ARGS%
goto end

:runMuleWithJikes
if not "%CLASSPATH%"=="" goto runMuleWithJikesAndClasspath
"%_JAVACMD%" %MULE_DEBUG_OPTS% %MULE_OPTS% -classpath "%MULE_HOME%\bin\mule-launcher.jar" "-Dorg.mule.home=%MULE_HOME%" -Dorg.mule.main.class=%MULE_MAIN% "-Djikes.class.path=%JIKESPATH%" org.mule.tools.launcher.Launcher %MULE_ARGS% %MULE_CMD_LINE_ARGS%
goto end

:runMuleWithJikesAndClasspath
"%_JAVACMD%" %MULE_DEBUG_OPTS% %MULE_OPTS% -classpath "%MULE_HOME%\bin\mule-launcher.jar" "-Dorg.mule.home=%MULE_HOME%" -Dorg.mule.main.class=%MULE_MAIN% "-Djikes.class.path=%JIKESPATH%" org.mule.tools.launcher.Launcher %MULE_ARGS%  -lib "%CLASSPATH%" %MULE_CMD_LINE_ARGS%
goto end

:end
set _JAVACMD=
set MULE_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal

:mainEnd
if exist "%HOME%\mulerc_post.bat" call "%HOME%\mulerc_post.bat"

