/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.MuleManager;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

public abstract class AbstractUMOManagerTestCase extends AbstractMuleTestCase
{
    private static boolean initialised = false;

    protected void doSetUp() throws Exception
    {
        if (!initialised)
        {
            getUMOManager();
            initialised = true;
        }
    }

    public abstract UMOManager getUMOManager() throws Exception;

    public void testConnectorLookup() throws Exception
    {
        UMOConnector connector = MuleManager.getRegistry().lookupConnector("testConnector");
        assertNotNull(connector);
        assertEquals(1, MuleManager.getRegistry().getConnectors().size());
        UMOConnector connector2 = getTestConnector();
        MuleManager.getRegistry().registerConnector(connector2);
        assertEquals(2, MuleManager.getRegistry().getConnectors().size());

        assertNull(MuleManager.getRegistry().lookupConnector("doesnotexist"));
    }

    public void testEndpointLookup() throws Exception
    {
        UMOEndpoint endpoint = MuleManager.getRegistry().lookupEndpoint("testEndpoint");
        assertNotNull(endpoint);
        assertEquals(1, MuleManager.getRegistry().getEndpoints().size());
        UMOEndpoint endpoint2 = getTestEndpoint("testProvider2", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        MuleManager.getRegistry().registerEndpoint(endpoint2);
        assertEquals(2, MuleManager.getRegistry().getEndpoints().size());

        UMOEndpoint endpoint3 = MuleManager.getRegistry().lookupEndpoint("doesnotexist");
        assertNull(endpoint3);
    }

    public void testTransformerLookup() throws Exception
    {
        UMOTransformer transformer = MuleManager.getRegistry().lookupTransformer("testTransformer");
        assertNotNull(transformer);
        assertEquals(1, MuleManager.getRegistry().getTransformers().size());
        UMOTransformer transformer2 = getTestTransformer();
        MuleManager.getRegistry().registerTransformer(transformer2);
        assertEquals(2, MuleManager.getRegistry().getTransformers().size());

        UMOTransformer transformer3 = MuleManager.getRegistry().lookupTransformer("doesnotexist");
        assertNull(transformer3);
    }

    public void testEndpointIdentifierLookup() throws Exception
    {
        String endpoint = MuleManager.getRegistry().lookupEndpointIdentifier("testEndpointURI", null);
        assertNotNull(endpoint);
        assertEquals("test://endpoint.test", endpoint);
        assertEquals(1, MuleManager.getRegistry().getEndpointIdentifiers().size());
        MuleManager.getRegistry().registerEndpointIdentifier("testEndpoint2", "endpointUri.test.2");
        assertEquals(2, MuleManager.getRegistry().getEndpointIdentifiers().size());

        String endpoint2 = MuleManager.getRegistry().lookupEndpointIdentifier("doesnotexist", null);
        assertNull(endpoint2);
    }

    public void testManagerProperties()
    {
        String value = (String)MuleManager.getInstance().getProperty("envProperty1");
        assertEquals("value1", value);
        assertEquals(1, MuleManager.getInstance().getProperties().size());
    }

//    public void testInterceptorStacks()
//    {
//        UMOInterceptorStack stack1 = MuleManager.getRegistry().lookupInterceptorStack("testInterceptorStack");
//        assertNotNull(stack1);
//        List interceptors = stack1.getInterceptors();
//        assertEquals(2, interceptors.size());
//
//        InterceptorStack stack2 = new InterceptorStack();
//        List interceptors2 = new ArrayList();
//        interceptors2.add(new LoggingInterceptor());
//        stack2.setInterceptors(interceptors2);
//
//        MuleManager.getRegistry().registerInterceptorStack("testInterceptors2", stack2);
//
//        assertEquals(1, MuleManager.getRegistry()
//            .lookupInterceptorStack("testInterceptors2")
//            .getInterceptors()
//            .size());
//
//        UMOInterceptorStack stack3 = MuleManager.getRegistry().lookupInterceptorStack("doesnotexist");
//        assertNull(stack3);
//    }

    public void testTrasactionSetting() throws Exception
    {
        assertNotNull(MuleManager.getInstance().getTransactionManager());
        try
        {
            MuleManager.getInstance().setTransactionManager(null);
            fail("cannot set tx manager once it has been set");
        }
        catch (Exception e)
        {
            // expected
        }
    }
}
