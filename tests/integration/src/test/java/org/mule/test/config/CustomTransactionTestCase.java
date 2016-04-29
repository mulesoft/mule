/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;

public class CustomTransactionTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
