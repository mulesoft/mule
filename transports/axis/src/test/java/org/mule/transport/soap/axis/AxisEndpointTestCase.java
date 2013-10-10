/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.registry.ServiceType;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.service.TransportServiceDescriptor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AxisEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testEndpoint() throws Exception
    {
        String url = "axis:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        EndpointURI endpointUri = new MuleEndpointURI(url, muleContext);
        endpointUri.initialise();

        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if
        // necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
                endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());

        url = "axis:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        endpointUri = new MuleEndpointURI(url, muleContext);
        endpointUri.initialise();

        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
                endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
    }

    @Test
    public void testEndpointWithUserInfo() throws Exception
    {
        String url = "axis:http://admin:pwd@www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        EndpointURI endpointUri = new MuleEndpointURI(url, muleContext);
        endpointUri.initialise();

        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if
        // necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
                endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("http://admin:****@www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
                endpointUri.toString());
    }

    @Test
    public void testEndpointFinder() throws Exception
    {
        String url = "soap:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        EndpointURI endpointUri = new MuleEndpointURI(url, muleContext);
        endpointUri.initialise();

        assertEquals("soap", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if
        // necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
                endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());

        TransportServiceDescriptor sd = (TransportServiceDescriptor)
                muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, "soap", null);
        if (sd != null)
        {
            //TODO TC: How do we assert this state in the new model?
            //assertEquals("axis", sd.getProtocol());
            //assertEquals("org.mule.transport.soap.axis.AxisConnector", sd.getConnector());
        }
    }
}
