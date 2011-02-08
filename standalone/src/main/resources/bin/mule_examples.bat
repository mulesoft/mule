@echo off
setlocal

set URL=http://localhost:18082/examples/

echo MULE_HOME is set to %MULE_HOME%

call %MULE_HOME%\bin\launcher.bat %MULE_HOME%\bin\mule_examples.groovy "%URL%"

if not ERRORLEVEL 1 goto 
echo "ERROR: %ERRORLEVEL%. Please check logs to see why Mule ESB is not starting"
goto :eof

:open_url
Start %URL%
goto :eof

:eof
