/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.module.client.MuleClient;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.mule.api.MessagingException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.cxf.testmodels.CustomFault;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;


import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;

public class CxfExceptionHandlingTestCase extends DynamicPortTestCase
{
    public void testDefaultComponentExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
                            .lookupObject("cxfDefaultInbound")).getAddress() + "?method=testCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (MessagingException e)
        {
            assertTrue(e.getCause().getCause() instanceof SoapFault);
        }
    }
    public void testMuleExceptionStrategyHandling() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
                            .lookupObject("cxfMuleESInbound")).getAddress() + "?method=testCxfException", "TEST", null);
//            fail("Exception expected");
        }
//        catch (MessagingException e)
//        {
//            assertTrue(e.getCause().getCause() instanceof SoapFault);
//        }
        finally 
        {
            MuleMessage msg = client.request("vm://fromES", 10000);
            assertNotNull(msg);
            assertNotNull(msg.getPayload());
            assertEquals("EXCEPTION STRATEGY INVOKED", msg.getPayloadAsString());
            System.out.println(msg);
        }
    }
    
    
    protected String getConfigResources()
    {
        return "onexception-conf.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

}
