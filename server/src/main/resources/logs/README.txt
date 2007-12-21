This directory is used for tracing the execution of the Mule server.

Application-level logging is configured in the file "conf/log4j.properties" (by 
default all output is sent to the console).

System-level logging is configured in the file "conf/wrapper.conf" (by default 
all output is sent to the file "logs/mule.log"

Note that, unless the application is run in the foreground (i.e., not as a 
daemon), this means that while the application itself is configured to send its 
output to the console, the wrapper receives the console output and sends it to 
the log file.

In addition to the application's output, the wrapper also sends any JVM-level or 
OS-level errors/warnings to the log file.  This means that if the JVM crashes 
and automatically restarts (enabled by default), the time and cause of the crash 
will remain in the log file after the JVM restarts.
