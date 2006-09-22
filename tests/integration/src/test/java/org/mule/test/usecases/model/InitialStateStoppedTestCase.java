/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.model;

import java.util.Iterator;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOMessageReceiver;

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
