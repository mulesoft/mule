1. Register Mule listener on a server level. Edit $CATALINA_HOME/conf/server.xml and add this line

    <Listener className="org.mule.module.tomcat.MuleTomcatListener" />

2. Copy Mule lib folder (without boot) as is to $CATALINA_HOME/mule-libs/ (create one if necessary). No need to flatten
   directories.

3. Copy mule-module-tomcat-<version>.jar to $CATALINA_HOME/mule-libs/mule/ (if not there already).

4. Copy the following libraries from $MULE_HOME/lib/boot/ to $CATALINA_HOME/mule-libs/opt/:

    jcl104-over-slf4j-1.5.0.jar
    log4j-1.2.14.jar
    slf4j-api-1.5.0.jar
    slf4j-log4j12-1.5.0.jar

5. Edit $CATALINA_HOME/conf/catalina.properties and add the following to the "common.loader" (separate by a comma):

    ${catalina.home}/mule-libs/user/*.jar,${catalina.home}/mule-libs/mule/*.jar,${catalina.home}/mule-libs/opt/*.jar

6. In your application's web.xml use the following listener (no need to bundle any of Mule jars, only custom code):

    <listener>
        <listener-class>org.mule.config.builders.DeployableMuleXmlContextListener</listener-class>
    </listener>


