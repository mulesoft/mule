/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.api.transport.Connector;
import org.mule.api.transport.SessionHandler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HttpServiceOverridesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "http-service-overrides.xml";
    }

    @Test
    public void testSessionHandler() 
    {
        Connector connector = muleContext.getRegistry().lookupConnector("httpConnector");
        assertTrue(connector instanceof HttpConnector);
        
        HttpConnector httpConnector = (HttpConnector) connector;
        SessionHandler sessionHandler = httpConnector.getSessionHandler();
        assertTrue(sessionHandler instanceof TestSessionHandler);
    }

}
