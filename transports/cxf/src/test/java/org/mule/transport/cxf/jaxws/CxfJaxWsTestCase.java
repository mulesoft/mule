/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.jaxws;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

public class CxfJaxWsTestCase extends FunctionalTestCase
{
    public void testEchoService() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!",
            null);
        assertEquals("Hello!", result.getPayload());
    }

    public void testOneWay() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("cxf:http://localhost:63081/services/async?method=send", "Hello!",
            null);
        assertEquals(NullPayload.getInstance(), result.getPayload());
    }

    public void testHttpCall() throws Exception
    {
        HttpClient client =  new HttpClient();
        // The format in which CXF processes the request in Http GET is:
        // http://host/service/OPERATION/PARAM_NAME/PARAM_VALUE
        // In this case: http://localhost:63081/Echo/echo/text/hello
        // (service: Echo corresponds to the name in the mule config file: TC-HTTP-CALL.xml)
        HttpMethod httpMethod = new GetMethod("http://localhost:63081/services/Echo/echo/text/hello");
        // Http Status Code 200 means OK, the request has succeeded. (500 would indicate an error)
        assertEquals(200, client.executeMethod(httpMethod));
        // By default the class package - in its other way round - is used for the namespace:
        // Here, EchoServiceImpl classpath is: com\mulesource\test\connectors\transport\cxf
        // Therefore namespace should be: http://cxf.transport.connectors.test.mulesource.com
        assertEquals(
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                        "<ns2:echoResponse xmlns:ns2=\"http://testmodels.cxf.transport.mule.org/\">" +                        
                            "<text>hello</text>" +
                        "</ns2:echoResponse>" +
                    "</soap:Body>" +
                "</soap:Envelope>", httpMethod.getResponseBodyAsString());
    }

    protected String getConfigResources()
    {
        return "jaxws-conf.xml";
    }
}
