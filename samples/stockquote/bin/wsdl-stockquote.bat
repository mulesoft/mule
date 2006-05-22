@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..

REM Set your application specific classpath like this
SET CLASSPATH=%MULE_HOME%\samples\stockquote\conf;%MULE_HOME%\samples\stockquote\classes;

call %MULE_HOME%\bin\mule.bat -config ../conf/wsdl-mule-config.xml

SET CLASSPATH=