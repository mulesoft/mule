The scripts in this directory are used to control the Mule server from the command line.

+-------------+
| BASIC USAGE |
+-------------+

Linux / Unix
------------

./mule console [-config <your-config.xml>]

	(runs in the foreground, stop with Ctrl-C)

./mule start|stop|restart [-config <your-config.xml>]

	(runs in the background as a daemon)

Windows
-------

Mule.bat [-config <your-config.xml>]

+---------------------+
| CONFIGURATION FILES |
+---------------------+

If the "-config" parameter is not specified, a default file name of "mule-config.xml"
will be assumed.

You may optionally specify more than one file as a comma-separated list (this can be
useful for splitting up your Mule configuration to make it more manageable).

You can either place your configuration file(s) in the "../conf" directory prior to
startup or specify an explicit path to their location on the file system.

+---------------------+
| CLASSES & LIBRARIES |
+---------------------+

Any user classes or libraries used by your configuration should be placed in
"../lib/user" before starting up the server.

Please note that hot deployment is not yet supported, so you will need to restart
the server for configuration changes and/or new classes/libraries to take effect.

+-----------------------+
| CONFIGURATION BUILDER |
+-----------------------+

By default, the "org.mule.config.builders.MuleXmlConfigurationBuilder" class will be used
to interpret your configuration file(s).  If you wish to use a different configuration
builder, you can specify it using the "-builder" option.  For example, to use the Spring
configuration builder you could specify:

./mule start -config my-config.xml -builder org.mule.extras.spring.SpringConfigurationBuilder

+------------------+
| ADVANCED OPTIONS |
+------------------+

The scripts in this directory use the Java Service Wrapper
(http://wrapper.tanukisoftware.org) to control the Mule JVM from your native OS.
The wrapper provides many advanced options and features, for more information,
see http://wrapper.tanukisoftware.org/doc/english/launch.html
and http://wrapper.tanukisoftware.org/doc/english/properties.html
