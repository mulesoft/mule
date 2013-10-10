/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VMEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testUrlWithProvider() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("vm://some.queue?endpointName=vmProvider", muleContext);
        url.initialise();
        assertEquals(VMConnector.VM, url.getScheme());
        assertEquals("some.queue", url.getAddress());
        assertEquals("vmProvider", url.getEndpointName());
        assertEquals("vm://some.queue?endpointName=vmProvider", url.toString());
        assertEquals(1, url.getParams().size());
    }

}
