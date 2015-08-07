/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.junit.Ignore;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.SoapConstants;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.ExceptionUtils;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore("Requires tools.jar in the classpath")
public class MuleClientWSDLExternalTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testRequestResponse() throws Throwable
    {
        if (isOffline("org.mule.test.integration.client.MuleClientWSDLExternalTestCase.testRequestResponse()"))
        {
            return;
        }

        String input = "IBM";
        Map properties = new HashMap();
        properties.put(SoapConstants.SOAP_ACTION_PROPERTY, "${methodNamespace}${method}");
        properties.put(SoapConstants.METHOD_NAMESPACE_PROPERTY, "http://www.webserviceX.NET/");
        String url = "wsdl:http://www.webservicex.net/stockquote.asmx?WSDL&method=GetQuote";
        MuleMessage result = null;
        String resultPayload = StringUtils.EMPTY;

        try
        {
            MuleClient client = new MuleClient(muleContext);
            result = client.send(url, input, properties);
            resultPayload = (result != null ? result.getPayloadAsString() : StringUtils.EMPTY);
        }
        catch (MuleException e)
        {
            fail(ExceptionUtils.getStackTrace(e));
        }

        if (result != null)
        {
            logger.debug("The quote for " + input + " is: " + result.getPayload());
        }

        assertNotNull(result);
        assertTrue(resultPayload.startsWith("<StockQuotes><Stock><Symbol>IBM</Symbol>"));
    }

}
