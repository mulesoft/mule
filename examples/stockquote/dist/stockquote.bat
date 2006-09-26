@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..\..

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

ECHO (make sure you have configured your HTTP proxy if behind a firewall - see README.txt)
ECHO
ECHO The Stock Quote example is available in three variations:
ECHO   1. REST
ECHO   2. SOAP
ECHO   3. WSDL
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call %MULE_HOME%\bin\mule.bat -config .\conf\rest-config.xml
IF '%Choice%'=='2' call %MULE_HOME%\bin\mule.bat -config .\conf\soap-config.xml
IF '%Choice%'=='3' call %MULE_HOME%\bin\mule.bat -config .\conf\wsdl-config.xml
