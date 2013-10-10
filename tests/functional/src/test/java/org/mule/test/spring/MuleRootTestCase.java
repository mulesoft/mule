/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MuleRootTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/mule-root-test.xml";
    }

    @Test
    public void testModel() throws MuleException
    {
        assertNotNull("No model", muleContext.getRegistry().lookupModel("model"));
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("endpoint");
        assertNotNull("No endpoint", endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull("No address", address);
        assertEquals("value", address);
    }

}
