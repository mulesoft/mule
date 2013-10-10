/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EmbeddedMuleTestCase extends AbstractMuleTestCase
{
    @Test
    public void testStartup() throws Exception
    {
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(
            "org/mule/test/spring/mule-root-test.xml");
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        MuleContext context = muleContextFactory.createMuleContext(builder);
        // MuleContext must be started explicitly after MULE-1988
        assertFalse(context.isStarted());
        context.start();
        assertTrue(context.isStarted());

        final EndpointBuilder endpoint = context.getRegistry().lookupEndpointBuilder("endpoint");
        assertNotNull(endpoint);
        assertEquals("test://value", endpoint.buildInboundEndpoint().getEndpointURI().toString());
    }
}
