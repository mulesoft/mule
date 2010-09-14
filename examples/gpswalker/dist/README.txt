+--------------------+
| GPS Walker Example |
+--------------------+
Simulates plotting GPS coordinates

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive (mule-example-gpswalker.zip) to the
application folder ($MULE_HOME/apps) and start Mule. To access the web service 
invoke

    http://localhost:8081/services/gps/index.html

from your browser.

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

You can build the example by simply running "mvn".  This will compile the example 
classes and produce a zip file that will be copied into the application folder 
($MULE_HOME/apps).

(If you are unable to download the libraries it may be because you are behind a 
firewall and have not configured your build tool to use your HTTP proxy.  Please 
refer to the following information.)
    Ant users:     http://ant.apache.org/manual-beta/proxy.html
    Maven users:   http://maven.apache.org/guides/mini/guide-proxies.html