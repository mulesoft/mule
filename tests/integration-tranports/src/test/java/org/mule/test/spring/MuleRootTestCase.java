/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;

import org.junit.Test;

public class MuleRootTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/spring/mule-root-test.xml";
    }

    @Test
    public void testModel() throws MuleException
    {
        ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint("endpoint");
        assertNotNull("No endpoint", endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull("No address", address);
        assertEquals("value", address);
    }

    public EndpointFactory getEndpointFactory()
    {
        return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }
}
