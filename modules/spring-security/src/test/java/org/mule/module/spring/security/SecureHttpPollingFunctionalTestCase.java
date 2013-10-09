/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.context.notification.SecurityNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.util.concurrent.Latch;

public class SecureHttpPollingFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public SecureHttpPollingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "secure-http-polling-server-service.xml,secure-http-polling-client-service.xml"},
            {ConfigVariant.FLOW, "secure-http-polling-server-flow.xml,secure-http-polling-client-flow.xml"}
            });
    }

    @Test
    public void testPollingHttpConnectorSentCredentials() throws Exception
    {
        final Latch latch = new Latch();
        muleContext.registerListener(new SecurityNotificationListener<SecurityNotification>()
        {
            @Override
            public void onNotification(SecurityNotification notification)
            {
                latch.countDown();
            }
        });
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request("vm://toclient", 5000);
        assertNotNull(result);
        assertEquals("foo", result.getPayloadAsString());

        result = client.request("vm://toclient2", 1000);
        //This seems a little odd that we forward the exception to the outbound endpoint, but I guess users
        // can just add a filter
        assertNotNull(result);
        final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(401, status);
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    }
}
