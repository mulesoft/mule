1. Register Mule listener on a server level. Edit $CATALINA_HOME/conf/server.xml and add this line

    <Listener className="org.mule.module.tomcat.MuleTomcatListener" />

2. Copy Mule lib folder (without boot) as is to $CATALINA_HOME/mule-libs/ (create one if necessary). No need to flatten
   directories.

3. Copy mule-module-tomcat-<version>.jar to $CATALINA_HOME/mule-libs/mule/ (if not there already).

4. Copy the following libraries from $MULE_HOME/lib/boot/ to $CATALINA_HOME/mule-libs/opt/:

    jcl-over-slf4j-1.7.7.jar
    log4j-1.2-api-2.1.jar
    log4j-api-2.1.jar
    log4j-core-2.1.jar
    log4j-jcl-2.1.jar
    log4j-jul-2.1.jar
    log4j-slf4j-impl-2.1.jar
    slf4j-api-1.7.7.jar
    disruptor-3.3.0.jar

5. Edit $CATALINA_HOME/conf/catalina.properties and add the following to the "common.loader" (separate by a comma):

    ${catalina.home}/mule-libs/user/*.jar,${catalina.home}/mule-libs/mule/*.jar,${catalina.home}/mule-libs/opt/*.jar

6. In your application's web.xml use the following listener (no need to bundle any of Mule jars, only custom code):

    <listener>
        <listener-class>org.mule.config.builders.DeployableMuleXmlContextListener</listener-class>
    </listener>


