/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a> 
 * @version $Revision$
 */
public class MuleClientAxisExternalTestCase extends AbstractMuleTestCase
{
    public void testRequestResponse() throws Throwable
    {
        if(isOffline("org.mule.test.integration.client.MuleClientAxisExternalTestCase.testRequestResponse()")) {
            return;
        }

        String input =  "IBM";
        Map properties = new HashMap();
//        properties.put(AxisConnector.SOAP_ACTION_PROPERTY, "${methodNamespace}${method}");
//        properties.put(AxisConnector.METHOD_NAMESPACE_PROPERTY, "http://www.webserviceX.NET/");
        String url = "wsdl:http://www.webservicex.net/stockquote.asmx?WSDL&method=GetQuote";
        MuleClient client = null;
        client = new MuleClient();
        UMOMessage result = null;
        try {
            result = client.send(url, input,  properties);
        } catch (UMOException e) {
            e.printStackTrace();
        }
        System.out.println("The quote for " + input + " is: " + result.getPayload());    
        }

}
