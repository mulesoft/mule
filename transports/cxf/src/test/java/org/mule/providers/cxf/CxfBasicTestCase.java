/*
 * $Id: XFireBasicTestCase.java 6659 2007-05-23 04:05:51Z hasari $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.IOUtils;

import org.custommonkey.xmlunit.XMLUnit;

public class CxfBasicTestCase extends FunctionalTestCase
{
    private String echoWsdl;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        echoWsdl = IOUtils.getResourceAsString("xfire-echo-service.wsdl", getClass());
        XMLUnit.setIgnoreWhitespace(true);
    }

    public void testEchoService() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!",
            null);
        assertEquals("Hello!", result.getPayload());
    }

    public void testEchoServiceSynchronous() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("cxf:http://localhost:63083/services/Echo3?method=echo", "Hello!",
            null);
        assertEquals("Hello!", result.getPayload());
    }

    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive("http://localhost:63081/services/Echo?wsdl", 5000);
        assertNotNull(result.getPayload());
        XMLUnit.compareXML(echoWsdl, result.getPayload().toString());
    }

    protected String getConfigResources()
    {
        return "basic-conf.xml";
    }

}
