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

import org.mule.config.MuleProperties;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class AxisEndpointTestCase extends AbstractMuleTestCase
{

    public void testEndpoint() throws Exception
    {
        String url = "axis:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        UMOEndpointURI endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();
        
        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if
        // necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());

        url = "axis:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();
        
        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
    }

    public void testEndpointWithUserInfo() throws Exception
    {
        String url = "axis:http://admin:pwd@www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        UMOEndpointURI endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();
        
        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if
        // necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUsername());
        assertEquals("pwd", endpointUri.getPassword());
    }

    public void testEndpointFinder() throws Exception
    {
        String url = "soap:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        UMOEndpointURI endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();
        
        assertEquals("soap", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if
        // necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());

        TransportServiceDescriptor sd = (TransportServiceDescriptor)
                managementContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, "soap", null);
        if (sd != null)
        {
            //TODO TC: How do we assert this state in the new model?
            //assertEquals("axis", sd.getProtocol());
            //assertEquals("org.mule.providers.soap.axis.AxisConnector", sd.getConnector());
        }
    }
}
