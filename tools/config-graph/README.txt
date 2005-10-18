Mule Configuration Grapher
--------------------------
This is a cool little tool for generating  graphs for Mule configuration files

Getting started
---------------
You can generate configuration graphs for all Mule Sample applications
by running the following command in this directory (you need to have Maven installed) -

maven generate-samples

Usage
-----
To generate graphs for your own configuration files you can run

maven -Dfiles=my-mule-config.xml generate

Options
-------
-files      A comma-seperated list of Mule configuration files (required)
-outputdir  The directory to write the generated graphs to. Defaults to the current directory (optional)
-exec       The executable file used for Graph generation. Defaults to ./win32/dot.exe (optional)
-caption    Default caption for the generated graphs. Defaults to the 'id' attribute in the config file (optional)
?           Displays usage help