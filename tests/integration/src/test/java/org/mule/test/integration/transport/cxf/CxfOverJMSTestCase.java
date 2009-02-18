/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class CxfOverJMSTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/cxf/cxf-over-jms-config.xml";
    }

    public void testCxf() throws Exception
    {
        MuleClient client = new MuleClient();
        String req = 
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                    "<ns2:echo xmlns:ns2=\"http://simple.component.mule.org/\">" +                        
                        "<ns2:echo>hello</ns2:echo>" +
                    "</ns2:echo>" +
                "</soap:Body>" +
            "</soap:Envelope>";
        
        client.dispatch("jms://TestComponent", new DefaultMuleMessage(req));
        MuleMessage message = client.request("jms://testout", 10000);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayloadAsString().indexOf("return>hello") != -1);
    }

    public void testCxfClientOverJMS() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("clientEndpoint", new DefaultMuleMessage("hello"));
        MuleMessage message = client.request("jms://testout", 10000);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayloadAsString().equals("hello"));
    }
}
