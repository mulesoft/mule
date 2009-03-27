/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.IOUtils;

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;

public class CxfBasicTestCase extends FunctionalTestCase
{
    private String echoWsdl;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        echoWsdl = IOUtils.getResourceAsString("cxf-echo-service.wsdl", getClass());
        XMLUnit.setIgnoreWhitespace(true);
        try
        {
            XMLUnit.getTransformerFactory();
        }
        catch (TransformerFactoryConfigurationError e)
        {
            XMLUnit.setTransformerFactory(XMLUtils.TRANSFORMER_FACTORY_JDK5);
        }
    }

    public void testEchoService() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!",
            null);
        assertEquals("Hello!", result.getPayload());
    }

    public void testEchoServiceSynchronous() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("cxf:http://localhost:63083/services/Echo3?method=echo", "Hello!",
            null);
        assertEquals("Hello!", result.getPayload());
    }

    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.request("http://localhost:63081/services/Echo?wsdl", 5000);
        assertNotNull(result.getPayload());
        XMLUnit.compareXML(echoWsdl, result.getPayloadAsString());
    }

    protected String getConfigResources()
    {
        return "basic-conf.xml";
    }

}
