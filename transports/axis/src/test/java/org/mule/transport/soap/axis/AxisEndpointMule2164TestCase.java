/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AxisEndpointMule2164TestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testAxisHttpEndpointURICopy() throws Exception
    {

        // Create MuleEndpointURI and test values
        MuleEndpointURI endpointUri = new MuleEndpointURI("axis:http://localhost:8080?param=1", muleContext);
        endpointUri.initialise();
        assertEquals("http", endpointUri.getScheme());
        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        assertEquals("axis:http", endpointUri.getFullScheme());
        assertEquals("http://localhost:8080?param=1", endpointUri.getAddress());

        // Reconstruct MuleEndpointURI and test values
        EndpointURI newEndpointUri = new MuleEndpointURI(endpointUri);
        newEndpointUri.initialise();
        assertEquals("http", newEndpointUri.getScheme());
        assertEquals("axis", newEndpointUri.getSchemeMetaInfo());
        assertEquals("axis:http", newEndpointUri.getFullScheme());
        assertEquals("http://localhost:8080?param=1", newEndpointUri.getAddress());
        assertEquals(endpointUri, newEndpointUri);
    }

    @Test
    public void testAxisJmsEndpointURICopy() throws Exception
    {

        // Create MuleEndpointURI and test values
        MuleEndpointURI endpointUri = new MuleEndpointURI("axis:jms://myComponent", muleContext);
        assertEquals("jms", endpointUri.getScheme());
        assertEquals("axis", endpointUri.getSchemeMetaInfo());
        assertEquals("axis:jms", endpointUri.getFullScheme());
        assertEquals("jms://myComponent", endpointUri.getAddress());
        endpointUri.initialise();

        // Reconstruct MuleEndpointURI and test values
        EndpointURI newEndpointUri = new MuleEndpointURI(endpointUri);
        newEndpointUri.initialise();
        assertEquals("jms", newEndpointUri.getScheme());
        assertEquals("axis", newEndpointUri.getSchemeMetaInfo());
        assertEquals("axis:jms", newEndpointUri.getFullScheme());
        assertEquals("jms://myComponent", newEndpointUri.getAddress());
        assertEquals(endpointUri, newEndpointUri);
    }
}
