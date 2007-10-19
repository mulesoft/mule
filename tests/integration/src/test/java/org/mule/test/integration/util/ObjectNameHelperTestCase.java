/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.util;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class ObjectNameHelperTestCase extends AbstractMuleTestCase
{
    public void testEndpointAutomaticNames() throws Exception
    {

        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jms://cn=foo,name=queue", managementContext);
        managementContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.jms.cn.foo.name.queue", ep.getName());

        ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("jms://cn=foo,name=queue",
            managementContext);
        assertEquals("endpoint.jms.cn.foo.name.queue.1", ep.getName());

        // Test generating a unique name when there is a matching endpoint
        ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("vm://my.queue",
            managementContext);
        assertEquals("endpoint.vm.my.queue", ep.getName());
        ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "pop3://ross:secret@mail.mycompany.com?subject=foo", managementContext);
        assertEquals("endpoint.pop3.ross.mycompany.com", ep.getName());
    }

    public void testEndpointNames() throws Exception
    {
        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jms://cn=foo,name=queue?endpointName=foo", managementContext);
        managementContext.getRegistry().registerEndpoint(ep);
        assertEquals("foo", ep.getName());

        ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jms://cn=foo,name=queue?endpointName=this_is@aWierd-Name:x", managementContext);
        assertEquals("this.is.aWierd.Name.x", ep.getName());
        managementContext.getRegistry().registerEndpoint(ep);

        // Test generating a unique name when there is a matching endpoint
        ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jms://cn=foo,name=queue?endpointName=this_is@aWierd-Name:x", managementContext);
        assertEquals("this.is.aWierd.Name.x", ep.getName());
        ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jms://cn=foo,name=queue?endpointName=this____is+another=@Wierd----Name:x:::", managementContext);
        assertEquals("this.is.another.Wierd.Name.x", ep.getName());
    }

    public void testTestEndpoint() throws Exception
    {
        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://exception.listener", managementContext);
        managementContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.test.exception.listener", ep.getName());
    }
}
