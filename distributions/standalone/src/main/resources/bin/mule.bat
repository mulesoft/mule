@echo off
setlocal

rem Copyright (c) 1999, 2006 Tanuki Software Inc.
rem
rem Java Service Wrapper command based script
rem

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt

rem Configure remote Java debugging options here
rem Setting suspend=y will wait for you to connect before proceeding
set JPDA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0

rem Decide on the wrapper binary.
rem ###############################################################
rem Customized for Mule
rem ###############################################################

rem MULE_HOME must be set
if "%MULE_HOME%" == "" (
   echo You must set the MULE_HOME environment variable before starting Mule
   goto :eof
)

rem If MULE_BASE is not set, set it to MULE_HOME
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if "%MULE_APP%" == "" (
    set MULE_APP=mule
    set MULE_APP_LONG=Mule
) else (
    if "%MULE_APP_LONG%" == "" (
        set MULE_APP_LONG=%MULE_APP%
    )
)

set PATH=%PATH%;%MULE_HOME%\lib\native\profiler
set _WRAPPER_BASE=%MULE_HOME%\lib\boot\exec\wrapper

rem ###############################################################
set _WRAPPER_EXE=%_WRAPPER_BASE%-windows-x86-32.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_WRAPPER_BASE%-windows-x86-64.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_WRAPPER_BASE%.exe
if exist "%_WRAPPER_EXE%" goto validate
echo Unable to locate a Wrapper executable using any of the following names:
echo %_WRAPPER_BASE%-windows-x86-32.exe
echo %_WRAPPER_BASE%-windows-x86-64.exe
echo %_WRAPPER_BASE%.exe
pause
goto :eof

:validate
rem Find the requested command.
for /F %%v in ('echo %1^|findstr "^console$ ^start$ ^pause$ ^resume$ ^stop$ ^restart$ ^install$ ^remove"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    rem ###############################################################
    rem Customized for Mule
    rem ###############################################################
    echo Running in console/foreground mode by default, use Ctrl-C to exit...
    set COMMAND=:console
    rem pause
    rem goto :eof
    rem ###############################################################
) else (
    shift
)

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%_REALPATH%..\conf\wrapper.conf"

rem ###############################################################
rem Customized for Mule
rem ###############################################################
rem Export the location of this script.  This will be used in wrapper.conf
rem ATTENTION: %_REALPATH% contains a trailing path delimiter that will cause the wrapper to fail. Do not use that
rem (making MULE_EXE as absolute path to enable wrapper include additional configs correctly)
for /f "tokens=* delims= " %%a in ('attrib %MULE_HOME%\bin') do set MULE_EXE=%%a

rem add XML compatibility jars
set MULE_ENDORSED=-M-Djava.endorsed.dirs="%MULE_HOME%\lib\endorsed"

rem Mule options: Set the working directory to the current one and pass all command-line
rem options (-config, -builder, etc.) straight through to the main() method.
set MULE_OPTS=set.MULE_APP=%MULE_APP% set.MULE_APP_LONG=%MULE_APP_LONG% set.MULE_EXE="%MULE_EXE%" set.MULE_LIB=%MULE_LIB% wrapper.working.dir="%CD%" wrapper.app.parameter.1=%1 wrapper.app.parameter.2=%2  wrapper.app.parameter.3=%3  wrapper.app.parameter.4=%4  wrapper.app.parameter.5=%5  wrapper.app.parameter.6=%6  wrapper.app.parameter.7=%7  wrapper.app.parameter.8=%8 wrapper.app.parameter.9=%9

rem Adding additional jvm arguments to wrapper configuration if needed
call %MULE_HOME%\bin\launcher.bat %MULE_HOME%\bin\additional.groovy %_WRAPPER_CONF% "%JPDA_OPTS%" "%MULE_ENDORSED%" %*

:run

rem ###############################################################
rem
rem Run the application.
rem At runtime, the current directory will be that of wrapper.exe
rem
call :%COMMAND%
if errorlevel 1 pause
goto :eof

rem ###############################################################
rem Customized for Mule
rem ###############################################################

:console
"%_WRAPPER_EXE%" -c %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

:start
"%_WRAPPER_EXE%" -t %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

:pause
"%_WRAPPER_EXE%" -a %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

:resume
"%_WRAPPER_EXE%" -e %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

:stop
"%_WRAPPER_EXE%" -p %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

:install
"%_WRAPPER_EXE%" -i %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

:remove
"%_WRAPPER_EXE%" -r %_WRAPPER_CONF% %MULE_OPTS%
goto :eof

rem ###############################################################

:restart
call :stop
call :start
goto :eof

:exec
%*
goto :eof
