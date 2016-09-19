/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_REQUEST_END;

import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ConnectorMessageErrorNotificationTestCase extends AbstractNotificationTestCase
{
    private static final String FLOW_ID = "testFlow";
    private static final String MULE_CLIENT_ID = "MuleClient";

    private static final int TIMEOUT = 1000;
    private static final HttpRequestOptions GET_OPTIONS = HttpRequestOptionsBuilder.newOptions().method(HttpConstants.Methods.GET.name()).responseTimeout(TIMEOUT).disableStatusCodeValidation().build();

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "org/mule/test/integration/notifications/connector-message-error-notification-test-flow.xml"}
        });
    }

    public ConnectorMessageErrorNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void doTest() throws Exception
    {
        final String url = String.format("http://localhost:%s/path", port.getNumber());
        muleContext.getClient().send(url, getTestMuleMessage(), GET_OPTIONS);

        assertNotifications();
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_REQUEST_BEGIN, MULE_CLIENT_ID))
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_REQUEST_END, MULE_CLIENT_ID))
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_RECEIVED, FLOW_ID))
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_ERROR_RESPONSE, FLOW_ID));
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        // Nothing to validate
    }
}
