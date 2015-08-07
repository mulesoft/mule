/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.util.Preconditions;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.http.HttpServerFilter;

/**
 * {@link org.glassfish.grizzly.CompletionHandler}, responsible for asynchronous response writing
 */
public class ResponseCompletionHandler
        extends BaseResponseCompletionHandler
{

    private final FilterChainContext ctx;
    private final HttpResponsePacket httpResponsePacket;
    private final HttpContent httpResponseContent;
    private final ResponseStatusCallback responseStatusCallback;
    private boolean isDone;
    private boolean contentSend;

    public ResponseCompletionHandler(final FilterChainContext ctx, final HttpRequestPacket httpRequestPacket, final HttpResponse httpResponse, ResponseStatusCallback responseStatusCallback)
    {
        Preconditions.checkArgument((!(httpResponse.getEntity() instanceof InputStreamHttpEntity)), "response entity cannot be input stream");
        this.ctx = ctx;
        this.httpResponsePacket = buildHttpResponsePacket(httpRequestPacket, httpResponse);
        this.httpResponseContent = buildResponseContent(httpResponse);
        this.responseStatusCallback = responseStatusCallback;
    }

    public HttpContent buildResponseContent(final HttpResponse httpResponse)
    {
        final HttpEntity body = httpResponse.getEntity();
        Buffer grizzlyBuffer = null;
        if (body != null && !(body instanceof EmptyHttpEntity))
        {
            if (body instanceof ByteArrayHttpEntity)
            {
                grizzlyBuffer = Buffers.wrap(ctx.getMemoryManager(), ((ByteArrayHttpEntity) body).getContent());
            }
            else
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("At this point only a ByteArray entity is allowed"));
            }
        }
        HttpContent.Builder contentBuilder = HttpContent.builder(httpResponsePacket);
        //For some reason, grizzly tries to send Transfer-Encoding: chunk even if the content-length is set.
        if (httpResponse.getHeaderValue(CONTENT_LENGTH.toLowerCase()) != null)
        {
            contentBuilder.last(true);
        }
        return contentBuilder.content(grizzlyBuffer).build();
    }

    /**
     * Start the sending the response asynchronously
     *
     * @throws java.io.IOException
     */
    public void start() throws IOException
    {
        sendResponse();
    }

    /**
     * Send the next part of the response
     *
     * @throws java.io.IOException
     */
    public void sendResponse() throws IOException
    {
        if (!contentSend)
        {
            contentSend = true;
            isDone = !httpResponsePacket.isChunked();
            ctx.write(httpResponseContent, this);
            return;
        }
        isDone = true;
        ctx.write(httpResponsePacket.httpTrailerBuilder().build(), this);
    }

    /**
     * Method gets called, when the message part was successfully sent.
     *
     * @param result the result
     */
    @Override
    public void completed(WriteResult result)
    {
        try
        {
            if (!isDone)
            {
                sendResponse();
            }
            else
            {
                ctx.notifyDownstream(HttpServerFilter.RESPONSE_COMPLETE_EVENT);
                resume();
            }
        }
        catch (IOException e)
        {
            failed(e);
        }
    }

    /**
     * The method will be called, when http message transferring was canceled
     */
    @Override
    public void cancelled()
    {
        responseStatusCallback.responseSendFailure(new Exception("http response transferring cancelled"));
        resume();
    }

    /**
     * The method will be called, if http message transferring was failed.
     *
     * @param throwable the cause
     */
    @Override
    public void failed(Throwable throwable)
    {
        responseStatusCallback.responseSendFailure(throwable);
        resume();
    }

    /**
     * Resume the HttpRequestPacket processing
     */
    private void resume()
    {
        ctx.resume(ctx.getStopAction());
    }
}