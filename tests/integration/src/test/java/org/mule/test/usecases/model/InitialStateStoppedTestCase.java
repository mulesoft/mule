package org.mule.test.usecases.model;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOManager;

/**
 * Makes sure endpoints are not started if a component's initialState is set to "stopped".
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class InitialStateStoppedTestCase extends AbstractMuleTestCase {
	
	private UMOManager manager;
    
	public void testInitialStateStopped() throws Exception {    
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }

        // "TestComponent" is initially stopped (initialState="stopped").
		manager = new MuleXmlConfigurationBuilder().configure("org/mule/test/usecases/model/initial-state-stopped-config.xml");
		manager.start();
		
		// The connector should be started, but with no listeners registered.
		AbstractConnector connector = 
			(AbstractConnector) manager.lookupConnector("TestConnector");
		assertTrue(connector.isStarted());
        assertTrue(connector.getReceivers().isEmpty());   

        // Start the component.
        manager.getModel().startComponent("TestComponent");

		// The listeners should now be registered and started.
		assertTrue(connector.isStarted());
        assertFalse(connector.getReceivers().isEmpty());   

        // TODO The receivers should all be connected now, but the mock objects
        // don't actually set the connected property.  
//        Iterator it = connector.getReceivers().values().iterator();
//        while (it.hasNext()) {
//        	assertTrue(((UMOMessageReceiver) it.next()).isConnected());
//        }        
	}

	private static Log log = LogFactory.getLog(InitialStateStoppedTestCase.class);
}
