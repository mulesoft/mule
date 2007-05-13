/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.axis;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMODescriptor;

public class AxisOverJMSWithTransactionsTestCase extends FunctionalTestCase
{
    public void testTransactionPropertiesOnEndpoint() throws Exception {
        Object[] connectorArray = managementContext.getRegistry().getConnectors().values().toArray();
        AxisConnector connector = null;
        for (int i = 0; i < connectorArray.length; i++)
        {
            if (connectorArray[i] instanceof AxisConnector)
            {
                connector = (AxisConnector)connectorArray[i];
            }
        }      
        assertNotNull(connector);
        UMODescriptor axisDescriptor = managementContext.getRegistry().lookupService(connector.AXIS_SERVICE_COMPONENT_NAME);
        assertNotNull(axisDescriptor.getInboundRouter().getEndpoint("jms.TestComponent").getTransactionConfig());
    }
    
    public void testTransactionsOverAxis() throws Exception{
        MuleClient client = new MuleClient();
        client.dispatch("axis:jms://TestComponent?method=echo", new MuleMessage("test"));
        UMOMessage message = client.receive("jms://testout", 5000);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayloadAsString().equals("test"));
    }
    
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/axis/axis-over-jms-config.xml";
    }

}


