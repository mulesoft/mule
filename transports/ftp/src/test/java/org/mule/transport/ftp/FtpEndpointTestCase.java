/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftp;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FtpEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHostPortAndUserInfo() throws Exception
    {
        EndpointURI endpointUri = new MuleEndpointURI("ftp://admin:pwd@localhost:18080", muleContext);
        endpointUri.initialise();
        assertEquals("ftp", endpointUri.getScheme());
        assertEquals("ftp://localhost:18080", endpointUri.getAddress());
        assertNull(endpointUri.getEndpointName());
        assertEquals(18080, endpointUri.getPort());
        assertEquals("localhost", endpointUri.getHost());
        assertEquals("ftp://localhost:18080", endpointUri.getAddress());
        assertEquals(0, endpointUri.getParams().size());
        assertEquals("admin:pwd", endpointUri.getUserInfo());
        assertEquals("admin", endpointUri.getUser());
        assertEquals("pwd", endpointUri.getPassword());
        assertEquals("ftp://admin:****@localhost:18080", endpointUri.toString());
    }

}
