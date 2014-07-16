/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.functional;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class AxisSoapHeadersTestCase extends FunctionalTestCase
{
    private static final String EXPECTED_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Body><echoResponse soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><echoReturn xsi:type=\"xsd:string\">Test Message</echoReturn></echoResponse></soapenv:Body></soapenv:Envelope>";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "axis-soapheader-test.xml";
    }

    @Test
    public void testSoapRequest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("http.method", "POST");

        DefaultMuleMessage soapRequest = null;
        soapRequest = new DefaultMuleMessage(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:mule=\"http://www.muleumo.org/providers/soap/1.0\">"
            + "<soapenv:Header>"
            + "<Action>storeModuleInformation</Action>"
            + // this should be ignored
              "<mule:header>"
            + "<mule:MULE_REPLYTO>http://localhost:62182/reply</mule:MULE_REPLYTO>"
            + "</mule:header>"
            + "</soapenv:Header>"
            + "<soapenv:Body><echo soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><value0 xsi:type=\"soapenc:string\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">Test Message</value0></echo></soapenv:Body>"
            + "</soapenv:Envelope>", muleContext);

        MuleMessage reply = client.send("http://localhost:" + dynamicPort1.getNumber() + "/services/component", soapRequest, properties);

        // Put this in so that no spurious exceptions are thrown
        // TODO research and see why sometimes we get 404 or Connection refused
        // errors without this line. Note that the test completes even when the
        // exceptions are thrown.
        Thread.sleep(2000);

        assertEquals(EXPECTED_RESPONSE, reply.getPayloadAsString());
    }
}
