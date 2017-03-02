This directory is used for tracing the execution of the Mule container.

Top-level logger for the Mule container is configured in "$MULE_HOME/conf/log4j2.xml"

*** Xml format has higher priority and will be used if found or in presence of both xml and properties configs. ***

Log4j2 docs: http://logging.apache.org/log4j/2.x/

Container-level logging is written to the $MULE_HOME/logs/mule.log file.

Application-level logging supports 2 modes:

    1. No log4j configuration packaged with the app (either in app's lib or classes) -
       a rolling log file is automatically created by Mule:

              $MULE_HOME/logs/mule-app-<appName>.log

       The format is the same as the default Mule config.

    2. A log4j configuration is packaged with the app - no default log file is created
       by Mule and app has a full control (and responsibility) to configure logging.


*** TIP: use ${sys:mule.base} placeholder in log4j2.xml to resolve the Mule's directory (e.g. for file appenders). ***

In addition to the application's output, the wrapper also sends any JVM-level or 
OS-level errors/warnings to the log file.  This means that if the JVM crashes 
and automatically restarts (enabled by default), the time and cause of the crash 
will remain in the log file after the JVM restarts.
