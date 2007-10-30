/*
 * $Id: XFireBasicTestCase.java 6659 2007-05-23 04:05:51Z hasari $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;

import org.mule.providers.cxf.CxfConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.provider.UMOConnector;

public class ConfigurationTestCase extends FunctionalTestCase
{
    public void testBusConfiguration() throws Exception
    {
        boolean found = false;
        Collection connectors = managementContext.getRegistry().lookupObjects(UMOConnector.class);
        for (Iterator itr = connectors.iterator(); itr.hasNext();)
        {
            UMOConnector c = (UMOConnector) itr.next();

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
