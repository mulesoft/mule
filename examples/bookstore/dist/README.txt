+-------------------+
| Bookstore Example |
+-------------------+
This example illustrates:
1. How to interface with Mule via the Servlet Transport
2. How to interface with Mule via CXF Web Services
3. How to bundle Mule inside a webapp (deployable .WAR file)

It consists of two webapps consuming Mule services, one with Mule running 
inside it.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Bookstore+Example

+----------------------------------------+
| Running the example in Mule Standalone |
+----------------------------------------+
Simply copy the pre-built application archive (mule-example-bookstore-app.zip) 
to the application folder ($MULE_HOME/apps) and start Mule. 

If you want to receive emails when books are ordered, update the 
$MULE_HOME/apps/mule-example-bookstore/classes/email.properties file with your 
GMail credentials.  Then touch the 
$MULE_HOME/apps/mule-example-bookstore/mule-config.xml file to reload the app.

To access the webapps go to

    http://localhost:8083/bookstore/
    http://localhost:8083/bookstore-admin/

in your web browser.  From the bookstore webapp, you can search and order books.
From the bookstore-admin webapp, you can add books to the catalog and get stats 
on ordered books.

+------------------------------------------+
| Building the example for Mule Standalone |
+------------------------------------------+
First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

Then update the src/main/resources/email.properties file with your GMail 
credentials.

You can build the example by simply running "mvn".  This will compile the 
example classes and produce a zip file that will be copied into the application 
folder ($MULE_HOME/apps).

+---------------------------------------+
| Building the example for a Web Server |
+---------------------------------------+
You can build the bookstore-admin webapp as a fully deployable .WAR archive by 
running "mvn compile war:war".  This will produce a bookstore-admin.war archive 
in the target directory which you can deploy to any JEE web server such as 
Tomcat, Jetty, JBoss, etc.

The .WAR archive contains all needed Mule libraries and the following line
inside the web.xml file which will start up Mule when the webapp is deployed:

    <listener-class>org.mule.config.builders.MuleXmlBuilderContextListener</listener-class>
