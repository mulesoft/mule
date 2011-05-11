@echo off

rem ###############################################################
rem Check for java in path
rem ###############################################################

set FOUND=
set PROG=java.exe
for %%D in (%PROG%) do (set FOUND=%%~$PATH:D)

if "%FOUND%" == "" goto JAVA_NOT_IN_PATH

set JAVA=%FOUND%
goto HAS_JAVA

:JAVA_NOT_IN_PATH

rem ###############################################################
rem Check for JAVA environment variable
rem ###############################################################

if not "%JAVA%" == "" goto HAS_JAVA

rem ###############################################################
rem Check for JAVA_HOME environment variable
rem ###############################################################

if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

rem ###############################################################
rem Check registry for JRE
rem ###############################################################

for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Runtime Environment" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Runtime Environment\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Runtime Environment" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Runtime Environment\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

rem ###############################################################
rem Check registry for JDK
rem ###############################################################

for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Development Kit\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do set CurVer=%%B
for /F "skip=2 tokens=2*" %%A in ('REG QUERY "HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Development Kit\%CurVer%" /v JavaHome 2^>nul') do set JAVA_HOME=%%B
if not "%JAVA_HOME%" == "" goto HAS_JAVA_HOME

rem ###############################################################
rem All checks failed
rem ###############################################################

echo **********************************************************************
echo * ERROR: java.exe could not be found. Please install the JRE or JDK. *
echo * If it is already installed, configure the system variables PATH or *
echo * JAVA_HOME appropriately.                                           *
echo **********************************************************************

rem Exit the batch file only, not the entire cmd.exe process
pause
exit /b 1

:HAS_JAVA_HOME

echo Found JAVA_HOME = %JAVA_HOME%
set JAVA=%JAVA_HOME%\bin\java.exe

:HAS_JAVA

rem dynamically evaluate the name of the groovy jar
for /F %%v in ('dir /b "%MULE_BASE%"\lib\opt^| findstr groovy') do set GROOVY_JAR=%%v
set GROOVY_PATH=%MULE_BASE%\lib\opt\%GROOVY_JAR%

rem dynamically evaluate the name of the commons-cli jar
for /F %%v in ('dir /b "%MULE_BASE%"\lib\boot^| findstr commons-cli') do set COMMONS_CLI_JAR=%%v
set COMMONS_CLI_PATH=%MULE_BASE%\lib\boot\%COMMONS_CLI_JAR%

set cp=.;"%MULE_HOME%\conf"
set cp=%cp%;"%GROOVY_PATH%";"%COMMONS_CLI_PATH%"
"%JAVA%" -Dmule.home="%MULE_HOME%" -cp %cp% org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain --conf "%MULE_HOME%\bin\launcher.conf" %*
