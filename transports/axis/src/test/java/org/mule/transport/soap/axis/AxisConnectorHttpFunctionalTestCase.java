/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.api.MuleException;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.config.ExceptionHelper;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;

public class AxisConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{
    public static class ComponentWithoutInterfaces
    {
        public String echo(String msg)
        {
            return msg;
        }
    }

    public String getConfigResources()
    {
        return "axis-" + getTransportProtocol() + "-mule-config.xml";
    }

    protected String getTransportProtocol()
    {
        return "http";
    }

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
    public void testComponentWithoutInterfaces() throws Throwable
    {
        try
        {
            // TODO MULE-2228 Simplify this API
            Service c = MuleTestUtils.getTestService("testComponentWithoutInterfaces", ComponentWithoutInterfaces.class, null, muleContext, false);
            ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getEndpoint(
                new MuleEndpointURI(getComponentWithoutInterfacesEndpoint()), Endpoint.ENDPOINT_TYPE_RECEIVER);
            c.getInboundRouter().addEndpoint(ep);
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
