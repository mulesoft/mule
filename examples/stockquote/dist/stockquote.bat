@echo off
REM There is no need to call this if you set the MULE_HOME in your environment properties
REM but you must also define MULE_LIB for the example (see below)
REM or specify the config as a file: URI (see README.txt)
if "%MULE_HOME%" == "" SET MULE_HOME=..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

REM This extends the classpath to include the configuration directory
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

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config "stdio-config.xml,stockquote-rest-config.xml"
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config "stdio-config.xml,stockquote-wsdl-config.xml"
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -config "stdio-config.xml,stockquote-soap-config.xml"
