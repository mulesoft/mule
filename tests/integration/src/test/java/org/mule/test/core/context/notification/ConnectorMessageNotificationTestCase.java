/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_REQUEST_END;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;

public class ConnectorMessageNotificationTestCase extends AbstractNotificationTestCase
{
    private static final String FLOW_ID = "testFlow";
    private static final String MULE_CLIENT_ID = "MuleClient";

    private static final int TIMEOUT = 1000;
    private static final HttpRequestOptions GET_OPTIONS = HttpRequestOptionsBuilder.newOptions().method(HttpConstants.Methods.GET.name()).responseTimeout(TIMEOUT).disableStatusCodeValidation().build();

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/connector-message-notification-test-flow.xml";
    }

    @Override
    public void doTest() throws Exception
    {
        final String url = String.format("http://localhost:%s/path", port.getNumber());
        muleContext.getClient().send(url, getTestMuleMessage(), GET_OPTIONS);
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_REQUEST_BEGIN, MULE_CLIENT_ID))
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_REQUEST_END, MULE_CLIENT_ID))
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_RECEIVED, FLOW_ID))
                .parallel(new Node(ConnectorMessageNotification.class, MESSAGE_RESPONSE, FLOW_ID));
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        // Nothing to validate
    }
}
