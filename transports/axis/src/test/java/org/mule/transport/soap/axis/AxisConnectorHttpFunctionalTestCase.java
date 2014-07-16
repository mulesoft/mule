/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.config.ExceptionHelper;
import org.mule.tck.MuleTestUtils;

import org.junit.Test;

public class AxisConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{

    public static class ComponentWithoutInterfaces
    {
        public String echo(String msg)
        {
            return msg;
        }
    }

    @Override
    public String getConfigFile()
    {
        return "axis-" + getTransportProtocol() + "-mule-config.xml";
    }

    @Override
    protected String getTransportProtocol()
    {
        return "http";
    }

    @Override
    protected String getSoapProvider()
    {
        return "axis";
    }

    /**
     * The Axis service requires that the service implements at least one interface
     * This just tests that we get the correct exception if no interfaces are
     * implemented
     * 
     * @throws Throwable
     */
    @Test
    public void testComponentWithoutInterfaces() throws Throwable
    {
        try
        {
            // TODO MULE-2228 Simplify this API
            Service c = MuleTestUtils.getTestService("testComponentWithoutInterfaces", ComponentWithoutInterfaces.class, null, muleContext, false);
            InboundEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(getComponentWithoutInterfacesEndpoint());
            ((CompositeMessageSource) c.getMessageSource()).addSource(ep);
            muleContext.getRegistry().registerService(c);
            fail("Expected exception");
        }
        catch (MuleException e)
        {
            e = ExceptionHelper.getRootMuleException(e);
            assertTrue(e instanceof InitialisationException);
            assertTrue(e.getMessage(), e.getMessage().indexOf("must implement at least one interface") > -1);
        }
    }

}
