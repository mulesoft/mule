/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.RequestHandlerProvider;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;

/**
 * Grizzly filter that dispatches the request to the right request handler
 */
public class GrizzlyRequestDispatcherFilter extends BaseFilter
{

    private final RequestHandlerProvider requestHandlerProvider;

    GrizzlyRequestDispatcherFilter(final RequestHandlerProvider requestHandlerProvider)
    {
        this.requestHandlerProvider = requestHandlerProvider;
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException
    {
        final String scheme = (ctx.getAttributes().getAttribute(HttpConstants.Protocols.HTTPS.getScheme()) == null) ? HttpConstants.Protocols.HTTP.getScheme() : HttpConstants.Protocols.HTTPS.getScheme();
        final String ip = ((InetSocketAddress) ctx.getConnection().getLocalAddress()).getAddress().getHostAddress();
        final int port = ((InetSocketAddress) ctx.getConnection().getLocalAddress()).getPort();
        final HttpContent httpContent = ctx.getMessage();
        final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();

        final GrizzlyHttpRequestAdapter httpRequest = new GrizzlyHttpRequestAdapter(ctx, httpContent);
        HttpRequestContext requestContext = new HttpRequestContext(httpRequest, (InetSocketAddress) ctx.getConnection().getPeerAddress(), scheme);
        final RequestHandler requestHandler = requestHandlerProvider.getRequestHandler(ip, port, httpRequest);
        requestHandler.handleRequest(requestContext, new HttpResponseReadyCallback()
        {
            @Override
            public void responseReady(HttpResponse httpResponse, ResponseStatusCallback responseStatusCallback)
            {
                try
                {
                    if (httpResponse.getEntity() instanceof InputStreamHttpEntity)
                    {
                        new ResponseStreamingCompletionHandler(ctx, request, httpResponse, responseStatusCallback).start();
                    }
                    else
                    {
                        new ResponseCompletionHandler(ctx, request, httpResponse, responseStatusCallback).start();
                    }
                }
                catch (Exception e)
                {
                    responseStatusCallback.responseSendFailure(e);
                }
            }
        });
        return ctx.getSuspendAction();
    }
}
