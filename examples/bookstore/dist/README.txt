+--------------------------+
| Bookstore Webapp Example |
+--------------------------+
This example illustrates:
1. How to start up Mule inside a Webapp
2. How to interface with Mule via the Servlet Transport
3. How to interface with Mule via CXF Web Services

It consists of two webapps consuming Mule services, one with Mule running 
inside it.

For more information, refer to 
http://mule.mulesource.org/display/MULE2INTRO/Bookstore+Example

+---------------------+
| Running the example |
+---------------------+
You can build the example with Maven by simply running "mvn" from this directory

The WAR files will be generated in the following locations, which can then be 
deployed to any standard JEE web server:

  admin-web/target/bookstore-admin.war
  bookstore-web/target/bookstore.war

