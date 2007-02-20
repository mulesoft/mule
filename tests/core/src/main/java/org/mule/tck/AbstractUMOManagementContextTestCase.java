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

import org.mule.umo.UMOManagementContext;

public abstract class AbstractUMOManagementContextTestCase extends AbstractMuleTestCase
{
    private static boolean initialised = false;

    protected void doSetUp() throws Exception
    {
        if (!initialised)
        {
            managementContext = getUMOManagementContext();
            initialised = true;
        }
    }

    public abstract UMOManagementContext getUMOManagementContext() throws Exception;

//    public void testConnectorLookup() throws Exception
//    {
//        UMOConnector connector = managementContext.getRegistry().lookupConnector("testConnector");
//        assertNotNull(connector);
//        assertEquals(1, managementContext.getConnectors().size());
//        UMOConnector connector2 = getTestConnector();
//        managementContext.registerConnector(connector2);
//        assertEquals(2, managementContext.getConnectors().size());
//
//        assertNull(managementContext.getRegistry().lookupConnector("doesnotexist"));
//    }
//
//    public void testEndpointLookup() throws Exception
//    {
//        UMOEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("testEndpoint");
//        assertNotNull(endpoint);
//        assertEquals(1, managementContext.getEndpoints().size());
//        UMOEndpoint endpoint2 = getTestEndpoint("testProvider2", UMOEndpoint.ENDPOINT_TYPE_SENDER);
//        managementContext.registerEndpoint(endpoint2);
//        assertEquals(2, managementContext.getEndpoints().size());
//
//        UMOEndpoint endpoint3 = managementContext.getRegistry().lookupEndpoint("doesnotexist");
//        assertNull(endpoint3);
//    }
//
//    public void testTransformerLookup() throws Exception
//    {
//        UMOTransformer transformer = managementContext.getRegistry().lookupTransformer("testTransformer");
//        assertNotNull(transformer);
//        assertEquals(1, managementContext.getTransformers().size());
//        UMOTransformer transformer2 = getTestTransformer();
//        managementContext.registerTransformer(transformer2);
//        assertEquals(2, managementContext.getTransformers().size());
//
//        UMOTransformer transformer3 = managementContext.getRegistry().lookupTransformer("doesnotexist");
//        assertNull(transformer3);
//    }
//
//    public void testEndpointIdentifierLookup() throws Exception
//    {
//        String endpoint = managementContext.getRegistry().lookupEndpointIdentifier("testEndpointURI", null);
//        assertNotNull(endpoint);
//        assertEquals("test://endpoint.test", endpoint);
//        assertEquals(1, managementContext.getEndpointIdentifiers().size());
//        managementContext.registerEndpointIdentifier("testEndpoint2", "endpointUri.test.2");
//        assertEquals(2, managementContext.getEndpointIdentifiers().size());
//
//        String endpoint2 = managementContext.getRegistry().lookupEndpointIdentifier("doesnotexist", null);
//        assertNull(endpoint2);
//    }
//
//    public void testManagerProperties()
//    {
//        String value = (String)managementContext.getProperty("envProperty1");
//        assertEquals("value1", value);
//        assertEquals(1, managementContext.getProperties().size());
//    }
//
//    public void testInterceptorStacks()
//    {
//        UMOInterceptorStack stack1 = managementContext.getRegistry().lookupInterceptorStack("testInterceptorStack");
//        assertNotNull(stack1);
//        List interceptors = stack1.getInterceptors();
//        assertEquals(2, interceptors.size());
//
//        InterceptorStack stack2 = new InterceptorStack();
//        List interceptors2 = new ArrayList();
//        interceptors2.add(new LoggingInterceptor());
//        stack2.setInterceptors(interceptors2);
//
//        managementContext.registerInterceptorStack("testInterceptors2", stack2);
//
//        assertEquals(1, managementContext
//            .lookupInterceptorStack("testInterceptors2")
//            .getInterceptors()
//            .size());
//
//        UMOInterceptorStack stack3 = managementContext.getRegistry().lookupInterceptorStack("doesnotexist");
//        assertNull(stack3);
//    }

    public void testTrasactionSetting() throws Exception
    {
        assertNotNull(managementContext.getTransactionManager());
        try
        {
            managementContext.setTransactionManager(null);
            fail("cannot set tx manager once it has been set");
        }
        catch (Exception e)
        {
            // expected
        }
    }
}
