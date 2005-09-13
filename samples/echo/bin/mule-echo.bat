@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
SET MULE_HOME=..\..\..

REM Set your application specific classpath like this
SET CLASSPATH=%MULE_HOME%\samples\echo\conf

call %MULE_HOME%\bin\mule.bat -config ../conf/mule-config.xml

SET CLASSPATH=