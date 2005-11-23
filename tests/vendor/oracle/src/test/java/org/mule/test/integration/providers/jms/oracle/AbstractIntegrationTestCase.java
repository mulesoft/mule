package org.mule.test.integration.providers.jms.oracle;

import oracle.jms.AQjmsSession;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.umo.manager.UMOManager;

/**
 * Configures and starts the Mule server for integration testing.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public abstract class AbstractIntegrationTestCase extends AbstractXmlTestCase {
    
	// The UMOManager is a singleton so we only reinitialize it when the server
	// configuration has changed.
    protected static UMOManager manager = null;
    protected static String configurationFiles = null;

    // Reference variables to the running Mule server's configuration.
    protected static OracleJmsConnector connector = null;
	protected static AQjmsSession session = null;
    
    public void setUp() throws Exception {   	
    	super.setUp();
    	
    	// The UMOManager is a singleton so we only reinitialize it when the server
    	// configuration has changed.
    	if ((manager == null) || 
    		(getConfigurationFiles().equals(configurationFiles) == false)){
    		
    		if (manager != null) {
    			//if (manager.isStarted()) manager.stop(); 
    			manager.dispose();
    		}
    		configurationFiles = getConfigurationFiles();
    		manager = new MuleXmlConfigurationBuilder().configure(configurationFiles);
    		manager.start();

	        // Get these reference variables once the Mule server has started.
	        connector = ((OracleJmsConnector) manager.lookupConnector("oracleJmsConnector"));
			session = (AQjmsSession) connector.getSession(false, false);
    	}
    }
    
    public void tearDown() throws Exception {   	   	
//		if (manager != null) {
//			//if (manager.isStarted()) manager.stop(); 
//			manager.dispose();
//		}
    	super.tearDown();
    }
    
    abstract protected String getConfigurationFiles();
}
