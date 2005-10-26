package org.mule.test.integration.providers.jms.oracle;

import java.io.StringReader;

import oracle.jms.AQjmsSession;

import org.mule.config.ReaderResource;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.manager.UMOManager;

import org.mule.providers.oracle.jms.*;

/**
 * Configures and starts the Mule server for integration testing.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class AbstractIntegrationTestCase extends AbstractXmlTestCase {
    
	protected UMOManager manager = null;
    protected OracleJmsConnector connector = null;
	protected AQjmsSession session = null;
    
    public void setUp() throws Exception {
    	super.setUp();
    	
    	// The UMOManager is a singleton so it only needs to be initialized once.
    	if (manager == null) {
    		manager = new MuleXmlConfigurationBuilder().configure(new ReaderResource[]{
    				new ReaderResource("Hard-coded server configuration", 
    									new StringReader(getServerConfiguration()))});
	        manager.start();
	        
	        // Get these reference variables once the Mule server has started.
	        connector = ((OracleJmsConnector) manager.lookupConnector("TestConnector"));
			session = (AQjmsSession) connector.getSession(false, false);
        }
    }
    
    protected String getServerConfiguration() {
    	return 
    	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE mule-configuration PUBLIC \"-//SymphonySoft //DTD mule-configuration XML V1.0//EN\" \"http://www.symphonysoft.com/dtds/mule/mule-configuration.dtd\">" +
    	"<mule-configuration id=\"TestConfiguration\" version=\"1.0\">" +
    	"<connector name=\"TestConnector\" className=\"org.mule.providers.oracle.jms.OracleJmsConnector\">" +
    	    "<properties>" +
    			"<property name=\"url\" value=\"" + TestConfig.DB_URL + "\" />" + 
    	        "<property name=\"username\" value=\"" + TestConfig.DB_USER + "\" />" +
    	        "<property name=\"password\" value=\"" + TestConfig.DB_PASSWORD + "\" />" +
    	    "</properties>" +
    	"</connector>" +
        "<transformers>" +
            "<transformer name=\"ObjectToJMSMessage\" className=\"org.mule.providers.jms.transformers.ObjectToJMSMessage\" returnClass=\"javax.jms.Message\" />" +
            "<transformer name=\"JMSMessageToObject\" className=\"org.mule.providers.jms.transformers.JMSMessageToObject\" returnClass=\"java.lang.Object\" />" +
            "<transformer name=\"StringToXMLMessage\" className=\"org.mule.providers.oracle.jms.transformers.StringToXMLMessage\" returnClass=\"oracle.jms.AdtMessage\" />" +
            "<transformer name=\"XMLMessageToString\" className=\"org.mule.providers.oracle.jms.transformers.XMLMessageToString\" returnClass=\"java.lang.String\" />" +
            "<transformer name=\"XMLMessageToDOM\" className=\"org.mule.providers.oracle.jms.transformers.XMLMessageToDOM\" returnClass=\"org.w3c.dom.Document\" />" +
            "<transformer name=\"XMLMessageToStream\" className=\"org.mule.providers.oracle.jms.transformers.XMLMessageToStream\" returnClass=\"java.io.InputStream\" />" +
        "</transformers>" +
        "</mule-configuration>";
    }
}
