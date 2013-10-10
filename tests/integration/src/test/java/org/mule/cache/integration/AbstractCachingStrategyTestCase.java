/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cache.integration;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Defines a contract test for {@link org.mule.cache.CachingStrategy}.
 * <p/>
 * Subclasses must provide a configuration of a cachingStrategy bean
 */
public abstract class AbstractCachingStrategyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/cache/integration/message-caching-config.xml," + getCachingStrategyConfigResource();
    }

    /**
     * @return comma separated list of the mule config file path's containing
     *         the declaration of the caching strategy bean and any extra
     *         needed configuration.
     */
    protected abstract String getCachingStrategyConfigResource();

    @Test
    public void testCachesMessageRequestResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();

        DefaultMuleMessage message1 = new DefaultMuleMessage("test1", (Map) null, muleContext);
        DefaultMuleMessage message2 = new DefaultMuleMessage("test2", (Map) null, muleContext);

        MuleMessage msg = client.send("vm://testRequestResponse", message1);
        assertEquals("0 Processed", msg.getPayload());

        msg = client.send("vm://testRequestResponse", message2);
        assertEquals("1 Processed", msg.getPayload());

        // Checks that resending message 1 gets the response form the cache
        msg = client.send("vm://testRequestResponse", message1);
        assertEquals("0 Processed", msg.getPayload());
    }

    @Test
    public void testCachesMessageOneWay() throws Exception
    {
        MuleClient client = muleContext.getClient();

        DefaultMuleMessage message1 = new DefaultMuleMessage("test3", (Map) null, muleContext);
        DefaultMuleMessage message2 = new DefaultMuleMessage("test4", (Map) null, muleContext);

        client.dispatch("vm://testOneWay", message1);
        MuleMessage msg = client.request("vm://output", 5000);
        assertEquals("0 Processed", msg.getPayload());

        client.dispatch("vm://testOneWay", message2);
        msg = client.request("vm://output", 5000);
        assertEquals("1 Processed", msg.getPayload());

        // Checks that resending message 1 gets the response form the cache
        client.dispatch("vm://testOneWay", message1);
        msg = client.request("vm://output", 5000);
        assertEquals("0 Processed", msg.getPayload());
    }
}
