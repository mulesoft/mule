/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.providers.soap.xfire.testmodels.MockServiceFactory;
import org.mule.providers.soap.xfire.testmodels.MockXFire;
import org.mule.tck.FunctionalTestCase;

public class XFireNamespaceHandlerTestCase extends FunctionalTestCase
{
    private static final String MOCK_SERVICE_CLASS = "org.mule.providers.soap.xfire.testmodels.MockService";

    protected String getConfigResources()
    {
        return "xfire-namespace-config.xml";
    }

    public void testConfig()
    {
        XFireConnector connector = 
            (XFireConnector)managementContext.getRegistry().lookupConnector("xfireConnector");
        
        assertNotNull(connector);
        assertEquals("org.codehaus.xfire.service.binding.MessageBindingProvider",
            connector.getBindingProvider());

        assertEquals(2, connector.getClientInHandlers().size());
        assertTrue(connector.getClientInHandlers().contains("org.codehaus.xfire.util.dom.DOMInHandler"));
        assertTrue(connector.getClientInHandlers().contains("org.codehaus.xfire.util.LoggingHandler"));
        
        assertEquals(1, connector.getClientOutHandlers().size());
        assertTrue(connector.getClientOutHandlers().contains("org.codehaus.xfire.util.dom.DOMOutHandler"));
        
        assertEquals(1, connector.getClientServices().size());
        assertTrue(
            connector.getClientServices().contains(MOCK_SERVICE_CLASS));
        
        assertEquals("org.codehaus.xfire.transport.dead.DeadLetterTransport",
            connector.getClientTransport());
        assertFalse(connector.isEnableJSR181Annotations());
        
        assertEquals(2, connector.getServerInHandlers().size());
        assertTrue(connector.getServerInHandlers().contains("org.codehaus.xfire.util.dom.DOMInHandler"));
        assertTrue(connector.getServerInHandlers().contains("org.codehaus.xfire.util.LoggingHandler"));
        
        assertEquals(1, connector.getServerOutHandlers().size());
        assertTrue(connector.getServerOutHandlers().contains("org.codehaus.xfire.util.dom.DOMOutHandler"));
        
        assertEquals(MOCK_SERVICE_CLASS, connector.getServiceTransport());
        assertEquals("org.mule.providers.soap.xfire.testmodels.MockTypeMappingRegistry",
            connector.getTypeMappingRegistry());
    }
    
    public void testInjected()
    {
        XFireConnector connector = 
            (XFireConnector)managementContext.getRegistry().lookupConnector("xfireConnector2");
        
        assertNotNull(connector);
        assertTrue(connector.getServiceFactory() instanceof MockServiceFactory);
        assertTrue(connector.getXfire() instanceof MockXFire);
    }        
}
