@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

ECHO The Hello example is available in three variations:
ECHO   1. Simple Configuration
ECHO   2. Spring-based Configuration
ECHO   3. Receive events via HTTP
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call %MULE_BASE%\bin\mule.bat -config .\conf\hello-config.xml
IF '%Choice%'=='2' call %MULE_BASE%\bin\mule.bat -config .\conf\hello-spring-config.xml -builder spring
IF '%Choice%'=='3' call %MULE_BASE%\bin\mule.bat -config .\conf\hello-http-config.xml
