/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transaction.TransactionConfig;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CustomTransactionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/custom-transaction-config.xml";
    }

    @Test
    public void testConfig1() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("testEndpoint1");
        assertNotNull(epb);
        InboundEndpoint iep = epb.buildInboundEndpoint();

        assertNotNull(iep.getTransactionConfig());
        assertTrue(iep.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
        assertEquals(TransactionConfig.ACTION_ALWAYS_BEGIN, iep.getTransactionConfig().getAction());
        assertEquals(4004, iep.getTransactionConfig().getTimeout());
    }

    @Test
    public void testConfig2() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("testEndpoint2");
        assertNotNull(epb);
        InboundEndpoint iep = epb.buildInboundEndpoint();

        assertNotNull(iep.getTransactionConfig());
        assertTrue(iep.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
        assertEquals(TransactionConfig.ACTION_ALWAYS_BEGIN, iep.getTransactionConfig().getAction());
        assertEquals(muleContext.getConfiguration().getDefaultTransactionTimeout(), iep.getTransactionConfig().getTimeout());
    }
}
