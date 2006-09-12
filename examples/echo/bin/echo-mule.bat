@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..

REM Set your application specific classpath like this
REM The echo does not use any user-defined classes, but just in case
REM you want to play with any
SET MULE_LIB=%MULE_HOME%\examples\echo\classes

call %MULE_HOME%\bin\mule.bat -config ../conf/echo-config.xml
