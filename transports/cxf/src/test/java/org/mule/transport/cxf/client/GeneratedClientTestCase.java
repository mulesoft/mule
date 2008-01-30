/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.client;

import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class GeneratedClientTestCase extends FunctionalTestCase
{
    public void testEchoService() throws Exception
    {
        // URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        // assertNotNull(wsdl);
        // SOAPService service = new SOAPService(wsdl, null);
        // Greeter soapPort = service.getSoapPort();
        //        
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body>" + "<test> foo </test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/Echo", msg, null);
        byte[] res = (byte[]) result.getPayload();
        String resString = new String(res);

        assertTrue(resString.indexOf("<test> foo </test>") != -1);
    }

    protected String getConfigResources()
    {
        return "bridge-conf.xml";
    }

}
