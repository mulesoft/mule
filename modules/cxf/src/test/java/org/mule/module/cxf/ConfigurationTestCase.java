/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.module.cxf.CxfConfiguration;
import org.mule.tck.FunctionalTestCase;

import java.util.Iterator;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;

public class ConfigurationTestCase extends FunctionalTestCase
{
    public void testBusConfiguration() throws Exception
    {
        CxfConfiguration config = muleContext.getRegistry().get("cxf");

        Bus cxfBus = ((CxfConfiguration) config).getCxfBus();
        boolean found = false;
        for (Iterator itr2 = cxfBus.getInInterceptors().iterator(); itr2.hasNext();)
        {
            Interceptor i = (Interceptor) itr2.next();
            if (i instanceof LoggingInInterceptor)
            {
                found = true;
                break;
            }
        }

        assertTrue("Did not find logging interceptor.", found);
    }

    protected String getConfigResources()
    {
        return "configuration-conf.xml";
    }

}
