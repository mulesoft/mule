/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JettyNamespaceHandlerTestCase extends FunctionalTestCase
{
    public JettyNamespaceHandlerTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "jetty-namespace-config.xml";
    }

    @Test
    public void checkConnectorProperties()
    {
        JettyHttpConnector connector =
            (JettyHttpConnector) muleContext.getRegistry().lookupConnector("jettyConnector");
        assertNotNull(connector.getConfigFile());
        assertEquals("jetty-config.xml", connector.getConfigFile());
        // Test a abstract connector property (MULE-5776)
        assertTrue(connector.isValidateConnections());
    }

    @Test
    public void checkSslConnectorProperties()
    {
        JettyHttpsConnector connector =
            (JettyHttpsConnector) muleContext.getRegistry().lookupConnector("jettySslConnector");
        //The full path gets resolved, we're just checking that the property got set
        assertTrue(connector.getKeyStore().endsWith("/serverKeystore"));
        assertEquals("muleserver", connector.getKeyAlias());
        assertEquals("mulepassword", connector.getKeyPassword());
        assertEquals("mulepassword", connector.getKeyStorePassword());
        //The full path gets resolved, we're just checking that the property got set
        assertTrue(connector.getClientKeyStore().endsWith("/clientKeystore"));
        assertEquals("mulepassword", connector.getClientKeyStorePassword());
        //The full path gets resolved, we're just checking that the property got set
        assertTrue(connector.getTrustStore().endsWith("/trustStore"));
        assertEquals("mulepassword", connector.getTrustStorePassword());
        // Test a abstract connector property (MULE-5776)
        assertTrue(connector.isValidateConnections());
    }

    /* See MULE-3603
    @Test
    public void testEndpointConfig() throws MuleException
    {
        InboundEndpoint endpoint =
            muleContext.getRegistry().lookupEndpointBuilder("endpoint").buildInboundEndpoint();
        assertNotNull(endpoint);
        // is the following test correct?
        // Can't test it now, the config for the endpoint isn't even valid
        assertEquals("http://localhost:60223/", endpoint.getEndpointURI().getAddress());
    }
    */

}
