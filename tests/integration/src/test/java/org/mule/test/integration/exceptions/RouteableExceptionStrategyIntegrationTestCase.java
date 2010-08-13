/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * @author estebanroblesluna
 * @since 2.2.6
 */
public class RouteableExceptionStrategyIntegrationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/routeable-exception-strategy-config.xml";
    }
    
    public void testNormalFlow() throws Exception 
    {        
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://in", 1, null);
        Thread.sleep(3000);
        MuleMessage message = client.request("vm://out?connector=queue", 1000);
        assertNotNull(message);
        assertEquals(2, message.getPayload());
    }
    
    public void testExceptionUsed() throws Exception 
    {
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://in", 2, null);
        Thread.sleep(3000);
        MuleMessage message = client.request("vm://exception?connector=queue", 1000);
        assertNotNull(message);
        assertEquals(4, message.getPayload());
    }
}
