/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.transport.Connector;
import org.mule.api.transport.SessionHandler;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

public class HttpServiceOverridesTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "http-service-overrides.xml";
    }

    public void testSessionHandler() 
    {
        Connector connector = muleContext.getRegistry().lookupConnector("httpConnector");
        assertTrue(connector instanceof HttpConnector);
        
        HttpConnector httpConnector = (HttpConnector) connector;
        SessionHandler sessionHandler = httpConnector.getSessionHandler();
        assertTrue(sessionHandler instanceof TestSessionHandler);
    }

}
