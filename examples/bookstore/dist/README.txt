+-------------------+
| Bookstore Example |
+-------------------+
This example illustrates:
1. How to interface with Mule via the Servlet Transport
2. How to interface with Mule via CXF Web Services

It consists of two webapps consuming Mule services, one with Mule running 
inside it.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3INTRO/Bookstore+Example

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive (mule-example-bookstore-app.zip) to the
application folder ($MULE_HOME/apps) and start Mule. 

If you want to receive emails when books are ordered, update the 
$MULE_HOME/apps/mule-example-bookstore/classes/email.properties file with your 
gmail credentials.  Then touch the $MULE_HOME/apps/mule-example-bookstore/mule-config.xml
file to reload the app.

To access the web service 
invoke

    http://localhost:8083/bookstore/
    http://localhost:8083/bookstore-admin/

from your browser.  From the bookstore webapp, you can search and order books.
From the bookstore-admin webapp, you can add books and get stats on ordered books.

+----------------------+
| Building the example |
+----------------------+
The only custom classes in here are used by CXF. These must be built using 
Java 1.5 because they use annotations.

First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

Then update the src/main/resources/email.properties file with your gmail credentials.

You can  build the example by simply running "mvn".  This will compile the example 
classes and produce a zip file that will be copied into the application folder 
($MULE_HOME/apps).
