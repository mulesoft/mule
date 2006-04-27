package org.mule.test.usecases.model;


import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Iterator;

/**
 * Makes sure endpoints are not started if a component's initialState is set to "stopped".
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class InitialStateStoppedTestCase extends AbstractMuleTestCase {

    public void testInitialStateStopped() throws Exception {

        // "TestComponent" is initially stopped (initialState="stopped").
        UMOManager manager = new MuleXmlConfigurationBuilder().configure("org/mule/test/usecases/model/initial-state-stopped-config.xml");
        UMOComponent c = manager.getModel().getComponent("TestComponent");

        assertNotNull(c);
        assertFalse(c.isStarted());
        manager.start();

        assertFalse(c.isStarted());

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

        assertTrue(c.isStarted());
        Iterator it = connector.getReceivers().values().iterator();
        while (it.hasNext()) {
            assertTrue(((UMOMessageReceiver) it.next()).isConnected());
        }
    }

}
