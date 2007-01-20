@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf
SET OPENEJB=openejb-core-1.0.jar

if exist "%MULE_BASE%\lib\user\%OPENEJB%" goto :mule
if exist "%MULE_HOME%\lib\user\%OPENEJB%" goto :mule
echo This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file.
goto :eof

:mule
call %MULE_BASE%\bin\mule.bat -main org.mule.samples.loanbroker.esb.Main
