/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.ServerNotification;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.FunctionalTestNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfCustomHttpHeaderTestCase extends AbstractServiceAndFlowTestCase implements FunctionalTestNotificationListener
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    private static final String REQUEST_PAYLOAD =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "<soap:Body>\n" +
            "<ns1:onReceive xmlns:ns1=\"http://functional.tck.mule.org/\">\n" +
            "    <ns1:arg0 xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">Test String</ns1:arg0>\n" +
            "</ns1:onReceive>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    private static final String SOAP_RESPONSE = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:onReceiveResponse xmlns:ns1=\"http://functional.tck.mule.org/\"><ns1:return xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">Test String Received</ns1:return></ns1:onReceiveResponse></soap:Body></soap:Envelope>";

    private List<MuleMessage> notificationMsgList = new ArrayList<MuleMessage>();
    private Latch latch = new Latch();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public CxfCustomHttpHeaderTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "headers-conf-service.xml"},
            {ConfigVariant.FLOW, "headers-conf-flow.xml"},
            {ConfigVariant.FLOW, "headers-conf-flow-httpn.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.registerListener(this);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        muleContext.unregisterListener(this);
    }

    @Test
    public void testCxf() throws Exception
    {
        String endpointAddress = "http://localhost:" + dynamicPort.getValue() + "/services/TestComponent";

        String myProperty = "myProperty";

        HashMap<String, Object> props = new HashMap<>();
        props.put(MuleProperties.MULE_USER_PROPERTY, "alan");
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "onReceive");
        props.put(myProperty, myProperty);

        MuleMessage reply = muleContext.getClient().send(
                String.format(endpointAddress),
                new DefaultMuleMessage(REQUEST_PAYLOAD, props, muleContext), HTTP_REQUEST_OPTIONS);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals(SOAP_RESPONSE, reply.getPayloadAsString());

        latch.await(3000, SECONDS);

        assertEquals(1, notificationMsgList.size());

        // MULE_USER should be allowed in
        assertEquals("alan", notificationMsgList.get(0).getInboundProperty(MuleProperties.MULE_USER_PROPERTY));

        // mule properties should be removed
        assertNull(notificationMsgList.get(0).getInboundProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        // custom properties should be allowed in
        assertEquals(myProperty, notificationMsgList.get(0).getInboundProperty(myProperty));
    }

    @Override
    public void onNotification(ServerNotification notification)
    {
        if (notification instanceof FunctionalTestNotification)
        {
            notificationMsgList.add(((FunctionalTestNotification) notification).getEventContext().getMessage());
            latch.release();
        }
        else
        {
            fail("invalid notification: " + notification);
        }
    }
}
