/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.mule.extras.client.MuleClient;
import org.mule.providers.soap.SoapConstants;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class MuleClientWSDLExternalTestCase extends AbstractMuleTestCase
{
    public void testRequestResponse() throws Throwable
    {
        if (isOffline("org.mule.test.integration.client.MuleClientAxisExternalTestCase.testRequestResponse()"))
        {
            return;
        }

        String input = "IBM";
        Map properties = new HashMap();
        properties.put(SoapConstants.SOAP_ACTION_PROPERTY, "${methodNamespace}${method}");
        properties.put(SoapConstants.METHOD_NAMESPACE_PROPERTY, "http://www.webserviceX.NET/");
        String url = "wsdl:http://www.webservicex.net/stockquote.asmx?WSDL&method=GetQuote";
        UMOMessage result = null;
        String resultPayload = StringUtils.EMPTY;

        try
        {
            MuleClient client = new MuleClient();
            result = client.send(url, input, properties);
            resultPayload = (result != null ? result.getPayloadAsString() : StringUtils.EMPTY);
        }
        catch (UMOException e)
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
