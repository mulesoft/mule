/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.jaxws;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class CxfJaxWsTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "jaxws-conf.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        String url = "cxf:http://localhost:" + dynamicPort.getNumber() + "/services/Echo?method=echo";

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send(url, "Hello!", null);
        assertEquals("Hello!", result.getPayload());
    }

    @Test
    public void testOneWay() throws Exception
    {
        String url = "cxf:http://localhost:" + dynamicPort.getNumber() + "/services/async?method=send";

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send(url, "Hello!", null);
        assertEquals(NullPayload.getInstance(), result.getPayload());
    }

    @Test
    public void testHttpCall() throws Exception
    {
        HttpClient client =  new HttpClient();
        // The format in which CXF processes the request in Http GET is:
        // http://host/service/OPERATION/PARAM_NAME/PARAM_VALUE
        // In this case: http://localhost:63081/Echo/echo/text/hello
        // (service: Echo corresponds to the name in the mule config file: TC-HTTP-CALL.xml)
        HttpMethod httpMethod = new GetMethod("http://localhost:" + dynamicPort.getNumber() + "/services/Echo/echo/text/hello");
        // Http Status Code 200 means OK, the request has succeeded. (500 would indicate an error)
        assertEquals(200, client.executeMethod(httpMethod));
        // By default the class package - in its other way round - is used for the namespace:
        // Here, EchoServiceImpl classpath is: com\mulesoft\test\connectors\module\cxf
        // Therefore namespace should be: http://cxf.transport.connectors.test.mulesoft.com
        assertEquals(
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                        "<ns2:echoResponse xmlns:ns2=\"http://testmodels.cxf.module.mule.org/\">" +
                            "<text>hello</text>" +
                        "</ns2:echoResponse>" +
                    "</soap:Body>" +
                "</soap:Envelope>", httpMethod.getResponseBodyAsString());
    }

    @Test
    public void testWebServiceContext() throws Exception
    {
        String url = "cxf:http://localhost:" + dynamicPort.getNumber() + "/services/Echo?method=ensureWebSerivceContextIsSet";

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send(url, TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, result.getPayload());
    }
}
