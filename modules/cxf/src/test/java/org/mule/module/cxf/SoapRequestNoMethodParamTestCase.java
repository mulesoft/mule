/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class SoapRequestNoMethodParamTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receive xmlns=\"http://www.muleumo.org\"><src xmlns=\"http://www.muleumo.org\">Test String</src></receive></soap:Body></soap:Envelope>";
    private static final String response = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:receiveResponse xmlns:ns1=\"http://services.testmodels.tck.mule.org/\"><ns1:return>Received: null</ns1:return></ns1:receiveResponse></soap:Body></soap:Envelope>";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    public SoapRequestNoMethodParamTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "soap-request-conf-service.xml"},
            {ConfigVariant.FLOW, "soap-request-conf-flow.xml"}});
    }

    @Test
    public void testCXFSoapRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage msg = client.send(
            ((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("httpInbound")).getAddress(),
            new DefaultMuleMessage(request, muleContext));        

        assertNotNull(msg);
        assertNotNull(msg.getPayload());
        assertEquals(response, msg.getPayloadAsString());
    }

}
