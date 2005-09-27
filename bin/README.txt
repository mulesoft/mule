You can use these scripts to run a Mule instance.  To run use -

mule -config <comma-separated list of config files>

To run with a different configuration builder such  as Spring or Groovy jst
specify the -builder parameter -

mule -config <config files> -builder org.mule.extras.spring.SpringConfigurationBuilder

The mule script loads all jars in the lib and lib/opt directories.  If you want to add your own
classpath you can either drop your jar into the lib directory of you can set the CLASSPATH
environment variable before running the script i.e.

For Linux and unix
export CLASSPATH=/myproject/classes:/myproject/conf
mule -config mule-config.xml

For Windows
set CLASSPATH=/myproject/classes;/myproject/conf
mule -config mule-config.xml

If you want to include a whole directory of jars, rether than listing them all in
the CLASSPATH variable you can specify the CUSTOM_LIB variable and point it to the
directory containing your jars.

You will see a 'patch' directory in the Mule lib directory/ This is useful for dropping in
patched Mule jars and will be included as the first entries in the classpath.

When the script is run it will set a MULE_HOME environment variable to the parent directory
from where the script was run.  If you have problems with files not being found set
the MULE_HOME environment variable explicitly to the root directory of your Mule distribution.