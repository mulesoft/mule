/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.junit.Assert.assertNotNull;
import org.mule.api.client.MuleClient;

public class EndpointMessageNotificationTestCase extends AbstractNotificationTestCase
{
    public static final String NO_ID = null;
    public static final String SERVICE_1_ID = "service-1";
    public static final String SERVICE_2_ID = "service-2";
    public static final String CLIENT_ID = "MuleClient";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/endpoint-message-notification-test-flow.xml";
    }

    @Override
    public void doTest() throws Exception
    {
        MuleClient client = muleContext.getClient();
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
