@echo off
setlocal
if "%MULE_HOME%" == "" SET MULE_HOME=%~dp0..\..
if "%MULE_BASE%" == "" SET MULE_BASE=%MULE_HOME%

if exist "%MULE_BASE%\apps\mule-example-stockquote" goto :run
echo This example requires you to build it before running it.  Please follow the instructions in the README.txt file.
goto :eof

:run
ECHO (make sure you have configured your HTTP proxy if behind a firewall - see README.txt)
ECHO.
ECHO The Stock Quote example is available in three variations:
ECHO   1. REST
ECHO   2. WSDL
ECHO   3. SOAP
SET /P Choice=Select the one you wish to execute and press Enter...

IF '%Choice%'=='1' call "%MULE_BASE%\bin\mule.bat" -config "%MULE_BASE%\apps\mule-example-stockquote\stockquote-rest-config.xml,%MULE_BASE%\apps\mule-example-stockquote\stdio-config.xml"
IF '%Choice%'=='2' call "%MULE_BASE%\bin\mule.bat" -config "%MULE_BASE%\apps\mule-example-stockquote\stockquote-wsdl-config.xml,%MULE_BASE%\apps\mule-example-stockquote\stdio-config.xml"
IF '%Choice%'=='3' call "%MULE_BASE%\bin\mule.bat" -config "%MULE_BASE%\apps\mule-example-stockquote\stockquote-soap-config.xml,%MULE_BASE%\apps\mule-example-stockquote\stdio-config.xml"
