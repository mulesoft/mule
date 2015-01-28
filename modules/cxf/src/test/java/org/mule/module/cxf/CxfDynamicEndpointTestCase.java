/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CxfDynamicEndpointTestCase extends FunctionalTestCase
{

    public static final String SAMPLE_REQUEST =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    private static int invocationCount = 0;

    @Rule
    public DynamicPort port = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "cxf-dynamic-endpoint-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        invocationCount = 0;
    }

    @Test
    public void sendsCorrectNotificationsWithCxfInsideEndpoint() throws Exception
    {
        doTest("http://localhost:" + port.getValue() + "/cxfInsideEndpoint");
    }

    @Test
    public void sendsCorrectNotificationsWithCxfOutsideEndpoint() throws Exception
    {
        doTest("http://localhost:" + port.getValue() + "/cxfOutsideEndpoint");
    }

    private void doTest(String url) throws MuleException
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put("DYNAMIC_PATH", "path1");
        client.send(url, SAMPLE_REQUEST, messageProperties);

        messageProperties.put("DYNAMIC_PATH", "path2");
        client.send(url, SAMPLE_REQUEST, messageProperties);

        messageProperties.put("DYNAMIC_PATH", "path1");
        client.send(url, SAMPLE_REQUEST, messageProperties);

        assertThat(invocationCount, equalTo(2));
    }

    public static class EndpointMetricsNotification implements EndpointMessageNotificationListener<EndpointMessageNotification>
    {

        @Override
        public void onNotification(EndpointMessageNotification notification)
        {
            if (notification.getAction() == EndpointMessageNotification.MESSAGE_SEND_END)
            {
                if (notification.getEndpoint().endsWith("path1"))
                {
                    invocationCount++;
                }
            }
        }
    }
}
