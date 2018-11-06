/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.runners.Parameterized.*;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.HttpConstants;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;

public class CxfClientProxyRepliesWithEmptyRequestResponse extends AbstractServiceAndFlowTestCase
{

    public static final int SC_GATEWAY_TIME = 504;
    public static final int SC_ACCEPTED = 202;

    public CxfClientProxyRepliesWithEmptyRequestResponse(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        System.setProperty("wsdl.uri", "ClientAndServiceProxy.wsdl");
    }

    @Rule
    public DynamicPort listenerDynamicPort = new DynamicPort("listenerPort");

    @Rule
    public DynamicPort requesterDynamicPort = new DynamicPort("requesterPort");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, "cxf-client-and-service-proxy.xml"}
        });
    }

    @Test
    public void testCxfProxyRepliesBackOnEmptyResponse() throws MuleException
    {
        System.setProperty("soapRequestResponseCode", new Integer(SC_GATEWAY_TIME).toString());

        String soapRequestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:test=\"http://test.Pablo.name/\">"
                     + "<soapenv:Header/>"
                     + "<soapenv:Body>"
                     + "<test:Hi/>"
                     + "</soapenv:Body>"
                     + "</soapenv:Envelope>";

          MuleClient client = muleContext.getClient();

          MuleMessage result = client.send("http://localhost:" + listenerDynamicPort.getNumber() + "/", getTestMuleMessage(soapRequestBody));

          assertThat(result.getOutboundProperty("http.status").toString(), is(new Integer(500).toString()));
    }

    @Test
    public void testCxfProxyTimeoutsOnAcceptedStatusCodeResponseAndEmptyResponse()
    {
        System.setProperty("soapRequestResponseCode", new Integer(SC_ACCEPTED).toString());
    }

}
