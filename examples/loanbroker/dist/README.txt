+---------------------+
| Loan Broker Example |
+---------------------+
This simple application is based on the Loan Broker example presented in the Enterprise
Integration Patterns book.

There are several variations of the same application which illustrate different architectural 
styles and Mule constructs.

For more information, refer to http://www.muledocs.org/Loan+Broker+Example

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by simply 
running "ant" or "mvn".  This will download any additional libraries, compile the example classes, 
produce a jar file(s), and copy everything to $MULE_HOME/lib/user, which is where your custom 
classes and configuration files should go.  

(If you are unable to download the libraries it may be because you are behind a firewall and have 
not configured your build tool to use your HTTP proxy.  Please refer to the following information.)
    Ant users:     http://ant.apache.org/manual-beta/proxy.html  
    Maven users:   http://maven.apache.org/guides/mini/guide-proxies.html 

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in this directory to run 
the example.
