/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.Connector;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.Collection;
import java.util.Iterator;


public abstract class AbstractAxisOverJMSWithTransactionsTestCase extends FunctionalTestCase
{

    public void testTransactionPropertiesOnEndpoint() throws Exception
    {
        Collection connectors = muleContext.getRegistry().lookupObjects(Connector.class);
        AxisConnector connector = null;
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            Connector candidate = (Connector) iterator.next();
            if (candidate instanceof AxisConnector)
            {
                connector = (AxisConnector) candidate;
            }
        }
        assertNotNull(connector);
        //This no longer works because the Axis descriptor name is made unique per connector
        //MuleDescriptor axisDescriptor = (MuleDescriptor)MuleManager.getInstance().lookupModel(ModelHelper.SYSTEM_MODEL).getDescriptor(connector.AXIS_SERVICE_COMPONENT_NAME);
        //assertNotNull(axisDescriptor.getInboundRouter().getEndpoint("jms.TestComponent").getTransactionConfig());
    }

    public void testTransactionsOverAxis() throws Exception{
        MuleClient client = new MuleClient();
        client.dispatch("axis:jms://TestComponent?method=echo", new DefaultMuleMessage("test"));
        MuleMessage message = client.request("jms://testout", 5000);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayloadAsString().equals("test"));
    }

}