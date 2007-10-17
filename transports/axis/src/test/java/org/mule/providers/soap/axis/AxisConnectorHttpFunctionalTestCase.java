/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.config.ExceptionHelper;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

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
     * The Axis service requires that the component implements at least one interface
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
            UMOComponent c = MuleTestUtils.getTestComponent("testComponentWithoutInterfaces", ComponentWithoutInterfaces.class, managementContext);
            UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getEndpoint(
                new MuleEndpointURI(getComponentWithoutInterfacesEndpoint()), UMOEndpoint.ENDPOINT_TYPE_RECEIVER,
                managementContext);
            c.getInboundRouter().addEndpoint(ep);
            managementContext.getRegistry().registerComponent(c, managementContext);
            c.start();
            fail("Expected exception");
        }
        catch (UMOException e)
        {
            e = ExceptionHelper.getRootMuleException(e);
            assertTrue(e instanceof InitialisationException);
            assertTrue(e.getMessage(), e.getMessage().indexOf("must implement at least one interface") > -1);
        }
    }
}
