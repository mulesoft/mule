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

bookstore-web$  mvn tomcat:run
admin-web$      mvn tomcat:run

Applications will then be available at:

bookstore-web: http://localhost:8888
admin-web:     http://localhost:8889
