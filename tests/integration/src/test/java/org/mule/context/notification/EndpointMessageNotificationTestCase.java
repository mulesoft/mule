/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;

public class EndpointMessageNotificationTestCase extends AbstractNotificationTestCase
{
    public static final String NO_ID = null;
    public static final String SERVICE_1_ID = "service-1";
    public static final String SERVICE_2_ID = "service-2";
    public static final String CLIENT_ID = "MuleClient";

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/notifications/endpoint-message-notification-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/notifications/endpoint-message-notification-test-flow.xml"}
        });
    }

    public EndpointMessageNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void doTest() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        assertNotNull(client.send("vm://in-1?connector=direct", "hello sweet world", null));
        client.dispatch("vm://in-2?connector=direct", "goodbye cruel world", null);
        assertNotNull(client.request("vm://out-2?connector=queue", 5000));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node().parallel(
            new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_SEND_BEGIN, CLIENT_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_SEND_END, CLIENT_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_RECEIVED,
                    SERVICE_1_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_RESPONSE,
                    SERVICE_1_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN,
                    CLIENT_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_DISPATCH_END,
                    CLIENT_ID)
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_RECEIVED,
                    SERVICE_2_ID))
             .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN,
                    SERVICE_2_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_DISPATCH_END,
                    SERVICE_2_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_REQUEST_END,
                    NO_ID))
            .parallel(
                new Node(EndpointMessageNotification.class, EndpointMessageNotification.MESSAGE_REQUEST_BEGIN,
                    NO_ID))); // a request notification bears no resource ID
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        // A
    }
}
