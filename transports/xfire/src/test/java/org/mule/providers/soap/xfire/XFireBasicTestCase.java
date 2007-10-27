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

import javax.wsdl.Binding;
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
        //This cause the noLocalBinding to fail, need to ask DanD about it
        //setDisposeManagerPerSuite(true);
    }

//    public void testEchoServiceMultiThreaded() throws Exception
//    {
//        final MuleClient client = new MuleClient(managementContext);
//        final List results = new ArrayList(50);
//        final ExceptionHolder exceptionHolder = new ExceptionHolder();
//        Runnable r = new Runnable() {
//
//            public void run()
//            {
//                for (int i = 0; i < 10; i++)
//                {
//                    try
//                    {
//                        UMOMessage result = client.send("xfire:http://localhost:63081/services/echoService?method=echo", "Hello!", null);
//                        //Throttling here to see if the steam gets closed before we can read it
//                        Thread.sleep(1000);
//                        System.out.println("received: " + result.getPayloadAsString() + " " + i);
//                        synchronized (results)
//                        {
//                            results.add(result.getPayloadAsString());
//                        }
//                    }
//                    catch (Exception e)
//                    {
//                        exceptionHolder.exceptionThrown(e);
//                        break;
//                    }
//                }
//            }
//
//        };
//
//        final int threads = 5;
//        for (int i = 0; i < threads; i++)
//        {
//            new Thread(r).start();
//        }
//
//        Thread.sleep(threads * 3000);
//        assertEquals(threads * 10, results.size());
//        assertEquals(0, exceptionHolder.getExceptions().size());
//        for (Iterator iterator = results.iterator(); iterator.hasNext();)
//        {
//            String s = (String) iterator.next();
//            assertEquals("Hello!", s);
//        }
//    }

    public void testEchoServiceSynchronous() throws Exception
    {
        // temporary, to compare values across OS
//        int port = 65423;
//        ServerSocket s = new ServerSocket(port);
//        logger.warn("Default timeout " + s.getSoTimeout());
//        Socket t = new Socket("localhost", port);
//        logger.warn("Default linger " + t.getSoLinger());
//        t.close();
//        s.close();

        MuleClient client = new MuleClient(managementContext);
        UMOMessage result = client.send("xfire:http://localhost:63083/services/echoService3?method=echo", "Hello!", null);
        assertEquals("Hello!", result.getPayload());
    }
    
    public void testNoLocalBinding() throws Exception
    {
        MuleClient client = new MuleClient(managementContext);
        WSDLReader wsdlReader = new WSDLReaderImpl();
        Definition wsdlDefinition = wsdlReader.readWSDL("http://localhost:63084/services/echoService4?wsdl");

        assertEquals(1, wsdlDefinition.getAllBindings().size());
        Binding binding = wsdlDefinition.getBinding(new QName("http://www.muleumo.org", "echoService4HttpBinding"));
        assertNotNull(binding);
        SOAPBinding soapBinding = (SOAPBinding) binding.getExtensibilityElements().get(0);
        assertEquals("http://schemas.xmlsoap.org/soap/http", soapBinding.getTransportURI());

        UMOMessage result = client.send("xfire:http://localhost:63084/services/echoService4?method=echo", "Hello!", null);
        assertEquals("Hello!", result.getPayload());
    }

    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient(managementContext);
        UMOMessage result = client.receive("http://localhost:63081/services/echoService?wsdl", 5000);
        assertNotNull(result.getPayload());
        XMLUnit.compareXML(echoWsdl, result.getPayloadAsString());
    }

    public void testListServices() throws Exception
    {
        MuleClient client = new MuleClient(managementContext);
        UMOMessage result = client.receive("http://localhost:63081/services/echoService?list", 5000);
        assertNotNull(result.getPayload());
        System.out.println(result.getPayloadAsString());
        //Note that Xfire wraps the HTML in Xml...
        assertTrue(result.getPayloadAsString().indexOf("<html><head>") > -1);
        assertTrue(result.getPayloadAsString().indexOf("<title>echoService") > -1);
    }

    protected String getConfigResources()
    {
        return "xfire-basic-conf.xml";
    }
}

