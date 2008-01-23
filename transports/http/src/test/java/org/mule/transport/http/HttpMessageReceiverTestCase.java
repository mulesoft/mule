/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.http.transformers.UMOMessageToHttpResponse;
import org.mule.util.CollectionUtils;

import com.mockobjects.dynamic.Mock;

public class HttpMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockComponent = new Mock(Component.class);
        mockComponent.expectAndReturn("getResponseTransformer", null);
        mockComponent.expectAndReturn("getResponseRouter", null);

        return new HttpMessageReceiver(endpoint.getConnector(),
            (Component)mockComponent.proxy(), endpoint);
    }

    public ImmutableEndpoint getEndpoint() throws Exception
    {
        endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "http://localhost:6789");
        ((Endpoint) endpoint).setResponseTransformers(CollectionUtils.singletonList(new UMOMessageToHttpResponse()));
        return endpoint;
    }
}
