@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
if "%MULE_HOME%" == "" SET MULE_HOME=..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM Any changes to the files in .\conf will take precedence over those deployed to %MULE_HOME%\lib\user
SET MULE_LIB=.\conf

ECHO (make sure you have configured your HTTP proxy if behind a firewall - see README.txt)
ECHO.
ECHO The Stock Quote example is available in three variations:
ECHO   1. REST
ECHO   2. WSDL
ECHO   3. SOAP
SET /P Choice=Select the one you wish to execute and press Enter...

REM Use the SpringConfigurationBuilder because the MuleXmlConfigurationBuilder (Commons Digester) has issues 
REM when merging config files.
REM Note: We can't use it for stockquote-soap-config.xml, because Spring chokes on the line:
REM   <property name="soapAction" value="${methodNamespace}${method}"/>

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config ".\conf\stdio-config.xml,.\conf\stockquote-rest-config.xml" -builder spring
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config ".\conf\stdio-config.xml,.\conf\stockquote-wsdl-config.xml" -builder spring
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -config ".\conf\stockquote-soap-config.xml" 
