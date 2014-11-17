/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.util;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.ObjectNameHelper;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringStartsWith;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ObjectNameHelperTestCase extends AbstractMuleContextTestCase
{

    public static final String UNIQUE_NAME_PREFIX = "unique-name-prefix";

    @Ignore
    @Test
    public void endpointAutomaticNames() throws Exception
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
    @Ignore
    public void endpointNameGenerationWithParams() throws Exception
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

    @Ignore
    @Test
    public void endpointNameGeneration() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://exception.listener");
        muleContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.test.exception.listener", ep.getName());
    }

    @Test
    public void uniqueNameGeneration() throws Exception
    {
        final ObjectNameHelper objectNameHelper = new ObjectNameHelper(muleContext);
        final String uniqueName = objectNameHelper.getUniqueName(UNIQUE_NAME_PREFIX);
        assertThat(uniqueName, startsWith(UNIQUE_NAME_PREFIX));
        final String secondUniqueName = objectNameHelper.getUniqueName(UNIQUE_NAME_PREFIX);
        assertThat(secondUniqueName, startsWith(UNIQUE_NAME_PREFIX));
        assertThat(uniqueName, not(is(secondUniqueName)));
        final String nextName = UNIQUE_NAME_PREFIX + "-2";
        muleContext.getRegistry().registerObject(nextName, "");
        final String thirdUniqueName = objectNameHelper.getUniqueName(UNIQUE_NAME_PREFIX);
        assertThat(thirdUniqueName, not(is(nextName)));
    }

}
