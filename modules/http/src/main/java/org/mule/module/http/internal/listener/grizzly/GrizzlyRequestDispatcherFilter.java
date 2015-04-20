/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static org.glassfish.grizzly.http.util.HttpStatus.CONINTUE_100;
import static org.glassfish.grizzly.http.util.HttpStatus.EXPECTATION_FAILED_417;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.api.HttpHeaders.Names.EXPECT;
import static org.mule.module.http.api.HttpHeaders.Values.CONTINUE;
import static org.mule.module.http.internal.listener.grizzly.MuleSslFilter.SSL_SESSION_ATTRIBUTE_KEY;

import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.ClientConnection;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.RequestHandlerProvider;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSession;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;

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
        final String scheme = (ctx.getAttributes().getAttribute(HTTPS.getScheme()) == null) ? HTTP.getScheme() : HTTPS.getScheme();
        final String ip = ((InetSocketAddress) ctx.getConnection().getLocalAddress()).getAddress().getHostAddress();
        final int port = ((InetSocketAddress) ctx.getConnection().getLocalAddress()).getPort();
        final HttpContent httpContent = ctx.getMessage();
        final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();

        // Handle Expect Continue
        if (request.requiresAcknowledgement())
        {
            final HttpResponsePacket.Builder responsePacketBuilder = HttpResponsePacket.builder(request);
            if (CONTINUE.equalsIgnoreCase(request.getHeader(EXPECT)))
            {
                responsePacketBuilder.status(CONINTUE_100.getStatusCode());
                HttpResponsePacket packet = responsePacketBuilder.build();
                packet.setAcknowledgement(true);
                ctx.write(packet);
                return ctx.getStopAction();
            }
            else
            {
                responsePacketBuilder.status(EXPECTATION_FAILED_417.getStatusCode());
                ctx.write(responsePacketBuilder.build());
                return ctx.getStopAction();
            }
        }

        final GrizzlyHttpRequestAdapter httpRequest = new GrizzlyHttpRequestAdapter(ctx, httpContent);
        HttpRequestContext requestContext = createRequestContext(ctx, scheme, httpRequest);
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

    private HttpRequestContext createRequestContext(FilterChainContext ctx, String scheme, GrizzlyHttpRequestAdapter httpRequest)
    {
        ClientConnection clientConnection;
        SSLSession sslSession = (SSLSession) ctx.getAttributes().getAttribute(SSL_SESSION_ATTRIBUTE_KEY);
        if (sslSession != null)
        {
            clientConnection = new ClientConnection(sslSession, (InetSocketAddress) ctx.getConnection().getPeerAddress());
        }
        else
        {
            clientConnection = new ClientConnection((InetSocketAddress) ctx.getConnection().getPeerAddress());
        }
        return new HttpRequestContext(httpRequest, clientConnection, scheme);
    }

}
