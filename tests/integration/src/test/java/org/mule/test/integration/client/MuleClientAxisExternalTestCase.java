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

import java.util.HashMap;
import java.util.Map;

import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleClientAxisExternalTestCase extends NamedTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }
    }

    public void testRequestResponse() throws Throwable
    {
        String input =  "IBM";
        Map properties = new HashMap();
        properties.put(AxisConnector.WSDL_URL_PROPERTY, "http://services.xmethods.net/soap/urn:xmethods-delayed-quotes.wsdl");
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
