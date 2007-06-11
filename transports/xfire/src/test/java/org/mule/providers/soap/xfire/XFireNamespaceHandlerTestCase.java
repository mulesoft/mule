/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.providers.soap.xfire.testmodels.MockServiceFactory;
import org.mule.providers.soap.xfire.testmodels.MockXFire;
import org.mule.tck.FunctionalTestCase;

import junit.framework.Assert;

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
        
        Assert.assertNotNull(connector);
        Assert.assertEquals("org.codehaus.xfire.service.binding.MessageBindingProvider",
            connector.getBindingProvider());

        Assert.assertEquals(2, connector.getClientInHandlers().size());
        Assert.assertTrue(connector.getClientInHandlers().contains("org.codehaus.xfire.util.dom.DOMInHandler"));
        Assert.assertTrue(connector.getClientInHandlers().contains("org.codehaus.xfire.util.LoggingHandler"));
        
        Assert.assertEquals(1, connector.getClientOutHandlers().size());
        Assert.assertTrue(connector.getClientOutHandlers().contains("org.codehaus.xfire.util.dom.DOMOutHandler"));
        
        Assert.assertEquals(1, connector.getClientServices().size());
        Assert.assertTrue(
            connector.getClientServices().contains(MOCK_SERVICE_CLASS));
        
        Assert.assertEquals("org.codehaus.xfire.transport.dead.DeadLetterTransport", 
            connector.getClientTransport());
        Assert.assertFalse(connector.isEnableJSR181Annotations());
        
        Assert.assertEquals(2, connector.getServerInHandlers().size());
        Assert.assertTrue(connector.getServerInHandlers().contains("org.codehaus.xfire.util.dom.DOMInHandler"));
        Assert.assertTrue(connector.getServerInHandlers().contains("org.codehaus.xfire.util.LoggingHandler"));
        
        Assert.assertEquals(1, connector.getServerOutHandlers().size());
        Assert.assertTrue(connector.getServerOutHandlers().contains("org.codehaus.xfire.util.dom.DOMOutHandler"));
        
        Assert.assertEquals(MOCK_SERVICE_CLASS, connector.getServiceTransport());
        Assert.assertEquals("org.mule.providers.soap.xfire.testmodels.MockTypeMappingRegistry",
            connector.getTypeMappingRegistry());
    }
    
    public void testInjected()
    {
        XFireConnector connector = 
            (XFireConnector)managementContext.getRegistry().lookupConnector("xfireConnector2");
        
        Assert.assertNotNull(connector);
        Assert.assertTrue(connector.getServiceFactory() instanceof MockServiceFactory);
        Assert.assertTrue(connector.getXfire() instanceof MockXFire);
    }        
}
