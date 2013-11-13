/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.AnnotatedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.component.DefaultJavaComponent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import javax.xml.namespace.QName;

import org.junit.Test;

/**
 * Test that configuration-based annotations are propagated to the appropriate runtime objects
 */
public class ConfigurationAnnotationsTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/annotations.xml";
    }

    @Test
    public void testAnnotations()
    {
        Transformer stb = muleContext.getRegistry().lookupTransformer("StringtoByteArray");
        assertNotNull(stb);
        assertEquals("stb-transformer", getDocName(stb));
        assertEquals("Convert a String to a Byte Array", getDocDescription(stb));
        EndpointBuilder in = muleContext.getRegistry().lookupEndpointBuilder("in");
        assertNotNull(in);
        assertEquals("inbound vm endpoint", getDocName(in));
        assertEquals("Accepts inbound messages", getDocDescription(in));
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("Bridge");
        assertNotNull(flow);
        assertEquals("Bridge flow", getDocName(flow));
        assertEquals("Main flow", getDocDescription(flow));
        DefaultJavaComponent echo = muleContext.getRegistry().lookupByType(DefaultJavaComponent.class).values().iterator().next();
        assertEquals("echo", getDocName(echo));
        ImmutableEndpoint ep = (ImmutableEndpoint) ((Flow)flow).getMessageSource();
        assertNotNull(ep);
        assertEquals("inbound flow endpoint", getDocName(ep));
        assertNull("Accepts inbound messages", getDocDescription(ep));
        OutboundEndpoint out = muleContext.getRegistry().lookupByType(OutboundEndpoint.class).values().iterator().next();
        assertNotNull(out);
        assertEquals("outbound vm endpoint", getDocName(out));
        assertEquals("Accepts outbound messages", getDocDescription(out));
    }

    protected String getDocName(Object obj)
    {
        assertTrue(obj instanceof AnnotatedObject);
        return (String) ((AnnotatedObject)obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/doc", "name"));
    }

     protected String getDocDescription(Object obj)
    {
        assertTrue(obj instanceof AnnotatedObject);
        return (String) ((AnnotatedObject)obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/doc", "description"));
    }
}
