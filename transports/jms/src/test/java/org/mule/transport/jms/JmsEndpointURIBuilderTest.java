/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import org.mule.api.endpoint.EndpointURI;

public class JmsEndpointURIBuilderTest
{

    @Test
    public void testWithArtemisFullyQualifiedQueueNameWithResourceInfo() throws Exception
    {
        JmsEndpointURIBuilder b = new JmsEndpointURIBuilder();
        EndpointURI u = b.build(new URI("jms://topic:address::queue1"), null);
        assertThat(u.getAddress(), equalTo("address::queue1"));
        assertThat(u.getResourceInfo(), equalTo("topic"));
    }

    @Test
    public void testWithArtemisFullyQualifiedQueueNameWithoutResourceInfo() throws Exception
    {
        JmsEndpointURIBuilder b = new JmsEndpointURIBuilder();
        EndpointURI u = b.build(new URI("jms://address::queue1"), null);
        assertThat(u.getAddress(), equalTo("address::queue1"));
        assertThat(u.getResourceInfo(), is(nullValue()));
    }
    
    @Test
    public void testWithArtemisFullyQualifiedQueueNameEndedWithColon() throws Exception
    {
        JmsEndpointURIBuilder b = new JmsEndpointURIBuilder();
        EndpointURI u = b.build(new URI("jms://queue:"), null);
        assertThat(u.getAddress(), equalTo(""));
    }
}