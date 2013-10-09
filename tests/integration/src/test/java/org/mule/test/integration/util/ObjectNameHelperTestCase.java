/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.util;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectNameHelperTestCase extends AbstractMuleContextTestCase
{
    
    @Test
    public void testEndpointAutomaticNames() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://cn=foo,name=queue");
        muleContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.test.cn.foo.name.queue", ep.getName());

        ep = muleContext.getEndpointFactory().getInboundEndpoint("test://cn=foo,name=queue");
        assertEquals("endpoint.test.cn.foo.name.queue.1", ep.getName());

        // Test generating a unique name when there is a matching endpoint
        ep = muleContext.getEndpointFactory().getInboundEndpoint("vm://my.queue");
        assertEquals("endpoint.vm.my.queue", ep.getName());
        ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "pop3://ross:secret@mail.mycompany.com?subject=foo");
        assertEquals("endpoint.pop3.ross.mycompany.com", ep.getName());
    }

    @Test
    public void testEndpointNames() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://cn=foo,name=queue?endpointName=foo");
        muleContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.test.cn.foo.name.queue", ep.getName());

        ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://cn=foo,name=queue?endpointName=this_is@aWierd-Name:x");
        assertEquals("this.is.aWierd.Name.x", ep.getName());
        muleContext.getRegistry().registerEndpoint(ep);

        // Test generating a unique name when there is a matching endpoint
        ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://cn=foo,name=queue?endpointName=this_is@aWierd-Name:x");
        assertEquals("this.is.aWierd.Name.x", ep.getName());
        ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://cn=foo,name=queue?endpointName=this____is+another=@Wierd----Name:x:::");
        assertEquals("this.is.another.Wierd.Name.x", ep.getName());
    }

    @Test
    public void testTestEndpoint() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://exception.listener");
        muleContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.test.exception.listener", ep.getName());
    }

}
