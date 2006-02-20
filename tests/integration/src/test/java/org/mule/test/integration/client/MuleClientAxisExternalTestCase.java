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
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleClientAxisExternalTestCase extends AbstractMuleTestCase
{
    public void testRequestResponse() throws Throwable
    {
        if(isOffline("org.mule.test.integration.client.MuleClientAxisExternalTestCase.testRequestResponse()")) return;

        String input =  "IBM";
        Map properties = new HashMap();
        properties.put(AxisConnector.WSDL_URL_PROPERTY, "http://services.xmethods.net/soap/urn:xmethods-delayed-quotes.wsdl");
        properties.put(AxisConnector.SOAP_ACTION_PROPERTY, "${methodNamespace}#${method}");
        properties.put(AxisConnector.METHOD_NAMESPACE_PROPERTY, "urn:xmethods-delayed-quotes");
        String url = "axis:http://services.xmethods.net:9090/soap?method=getQuote";
        MuleClient client = null;
        try {
            client = new MuleClient();
        } catch (UMOException e) {
            e.printStackTrace();
        }
        UMOMessage result = null;
        try {
            result = client.send(url, input,  properties);
        } catch (UMOException e) {
            e.printStackTrace();
        }
        System.out.println("The quote for " + input + " is: " + result.getPayload());    
        }

}
