/*
 * $Id:XFireBasicTestCase.java 7586 2007-07-19 04:06:50Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.IOUtils;

import com.ibm.wsdl.xml.WSDLReaderImpl;

import javax.wsdl.Definition;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.XMLUnit;

public class XFireBasicTestCase extends FunctionalTestCase
{
    private String echoWsdl;

    protected void doSetUp() throws Exception
    {
        echoWsdl = IOUtils.getResourceAsString("xfire-echo-service.wsdl", getClass());
        XMLUnit.setIgnoreWhitespace(true);
    }

    public void testEchoService() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("xfire:http://localhost:63081/services/echoService?method=echo", "Hello!", null);
        assertEquals("Hello!", result.getPayload());
    }
    public void testEchoServiceSynchronous() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("xfire:http://localhost:63083/services/echoService3?method=echo", "Hello!", null);
        assertEquals("Hello!", result.getPayload());
    }
    
    public void testNoLocalBinding() throws Exception
    {
        WSDLReader wsdlReader = new WSDLReaderImpl();
        Definition wsdlDefinition = wsdlReader.readWSDL("http://localhost:63084/services/echoService4?wsdl");
        assertEquals(1, wsdlDefinition.getAllBindings().size());
        SOAPBinding soapBinding = (SOAPBinding) wsdlDefinition.getBinding(new QName("http://www.muleumo.org", "echoService4HttpBinding")).getExtensibilityElements().get(0);
        assertEquals("http://schemas.xmlsoap.org/soap/http", soapBinding.getTransportURI());
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("xfire:http://localhost:63084/services/echoService4?method=echo", "Hello!", null);
        assertEquals("Hello!", result.getPayload());
    }
    
    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive("http://localhost:63081/services/echoService?wsdl", 5000);
        assertNotNull(result.getPayload());
        XMLUnit.compareXML(echoWsdl, result.getPayload().toString());
    }

    protected String getConfigResources()
    {
        return "xfire-basic-conf.xml";
    }
}

