/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.transport.Connector;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.cxf.CxfConnector;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;

public class ConfigurationTestCase extends FunctionalTestCase
{
    public void testBusConfiguration() throws Exception
    {
        boolean found = false;
        Collection connectors = muleContext.getRegistry().lookupObjects(Connector.class);
        for (Iterator itr = connectors.iterator(); itr.hasNext();)
        {
            Connector c = (Connector) itr.next();

            if (c instanceof CxfConnector)
            {
                System.out.println("Found connector");

                Bus cxfBus = ((CxfConnector) c).getCxfBus();

                for (Iterator itr2 = cxfBus.getInInterceptors().iterator(); itr2.hasNext();)
                {
                    Interceptor i = (Interceptor) itr2.next();
                    if (i instanceof LoggingInInterceptor)
                    {
                        found = true;
                        break;
                    }
                }
            }
        }

        assertTrue("Did not find logging interceptor.", found);
    }

    protected String getConfigResources()
    {
        return "configuration-conf.xml";
    }

}
