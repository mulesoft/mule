/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.util;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

public class ObjectNameHelperTestCase extends AbstractMuleTestCase
{
    public void testEndpointAutomaticNames() throws Exception
    {
        UMOEndpoint ep = new MuleEndpoint("jms://cn=foo,name=queue", true);
        managementContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.jms.cn.foo.name.queue", ep.getName());

        ep = new MuleEndpoint("jms://cn=foo,name=queue", true);
        assertEquals("endpoint.jms.cn.foo.name.queue.1", ep.getName());

        // Test generating a unique name when there is a matching endpoint
        ep = new MuleEndpoint("vm://my.queue", true);
        assertEquals("endpoint.vm.my.queue", ep.getName());
        ep = new MuleEndpoint("pop3://ross:secret@mail.mycompany.com?subject=foo", true);
        assertEquals("endpoint.pop3.ross.mycompany.com", ep.getName());
    }

    public void testEndpointNames() throws Exception
    {
        UMOEndpoint ep = new MuleEndpoint("jms://cn=foo,name=queue?endpointName=foo", true);
        managementContext.getRegistry().registerEndpoint(ep);
        assertEquals("foo", ep.getName());

        ep = new MuleEndpoint("jms://cn=foo,name=queue?endpointName=this_is@aWierd-Name:x", true);
        assertEquals("this.is.aWierd.Name.x", ep.getName());
        managementContext.getRegistry().registerEndpoint(ep);

        // Test generating a unique name when there is a matching endpoint
        ep = new MuleEndpoint("jms://cn=foo,name=queue?endpointName=this_is@aWierd-Name:x", true);
        assertEquals("this.is.aWierd.Name.x", ep.getName());
        ep = new MuleEndpoint("jms://cn=foo,name=queue?endpointName=this____is+another=@Wierd----Name:x:::",
            true);
        assertEquals("this.is.another.Wierd.Name.x", ep.getName());
    }

    public void testTestEndpoint() throws Exception
    {
        UMOEndpoint ep = new MuleEndpoint("test://exception.listener", true);
        managementContext.getRegistry().registerEndpoint(ep);
        assertEquals("endpoint.test.exception.listener", ep.getName());
    }
}
