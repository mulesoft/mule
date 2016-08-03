/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.StringUtils;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("ArtifactClassloaderTestRunner groovy error")
public class InOptionalOutTestCase extends AbstractIntegrationTestCase
{
    public static final long TIMEOUT = 3000;

    @Rule
    public DynamicPort port = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out-flow.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        String listenerUrl = format("http://localhost:%s/", port.getNumber());
        MuleMessage result = client.send(listenerUrl, "some data", null);
        assertNotNull(result);
        assertEquals(StringUtils.EMPTY, getPayloadAsString(result));

        Map<String, Serializable> props = new HashMap<>();
        props.put("foo", "bar");
        result = client.send(listenerUrl, "some data", props);
        assertNotNull(result);
        assertEquals("foo header received", getPayloadAsString(result));
    }
}
