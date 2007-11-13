/*
 * $Id: XFireSoapEndpointTestCase.java 3904 2006-11-17 21:06:02Z holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.config.MuleProperties;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class CxfSoapEndpointTestCase extends AbstractMuleTestCase
{
    public void testEndpoint() throws Exception
    {
        String url = "cxf:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        UMOEndpointURI endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();

        assertEquals("cxf", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());

        url = "cxf:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();

        assertEquals("cxf", endpointUri.getSchemeMetaInfo());
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
    }

    public void testEndpointWithUserInfo() throws Exception
    {
        String url = "cxf:http://admin:pwd@www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        UMOEndpointURI endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();

        assertEquals("cxf", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
    }

    public void testEndpointFinder() throws Exception
    {
        String url = "soap:http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2";
        UMOEndpointURI endpointUri = new MuleEndpointURI(url);
        endpointUri.initialise();

        assertEquals("soap", endpointUri.getSchemeMetaInfo());
        // it's up to the client to actually strip off the method name if necessary
        assertEquals("http://www.xmethods.net/wsdl/query.wsdl?method=getSomething&param1=1&param2=2",
            endpointUri.getAddress());
        assertEquals("getSomething", endpointUri.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY));
        assertEquals(3, endpointUri.getParams().size());
    }
}
