/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static org.glassfish.grizzly.http.HttpServerFilter.RESPONSE_COMPLETE_EVENT;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.DefaultMuleException;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * {@link org.glassfish.grizzly.CompletionHandler}, responsible for asynchronous http response transferring
 * when the response body is an input stream.
 */
public class ResponseStreamingCompletionHandler
        extends BaseResponseCompletionHandler
{
    private Integer objectCounter = Integer.valueOf(0);
    private final MemoryManager memoryManager;
    private final HttpResponsePacket httpResponsePacket;
    private final PositionInputStream inputStream;
    private final ResponseStatusCallback responseStatusCallback;
    private final ClassLoader loggerClassLoader;
    private volatile boolean isDone;

    public static final String MULE_CLASSLOADER = "MULE_CLASSLOADER";
    public boolean isFirst = true;
    int invocatioCter = 0;

    public ResponseStreamingCompletionHandler(final FilterChainContext ctx,
                                              final HttpRequestPacket request, final HttpResponse httpResponse, ResponseStatusCallback responseStatusCallback)
    {

        super(ctx);
        Preconditions.checkArgument((httpResponse.getEntity() instanceof InputStreamHttpEntity), "http response must have an input stream entity");
        httpResponsePacket = buildHttpResponsePacket(request, httpResponse);
        inputStream = new PositionInputStream( ((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream() );
        memoryManager = ctx.getConnection().getTransport().getMemoryManager();
        this.responseStatusCallback = responseStatusCallback;
        loggerClassLoader = Thread.currentThread().getContextClassLoader();
        synchronized (objectCounter) {
            objectCounter = objectCounter+ 1;
        }

    }

    @Override
    protected void doStart() throws IOException
    {
        sendInputStreamChunk();
    }

    public void sendInputStreamChunk() throws IOException
    {
        System.out.println("Entered sendInputStreamChunk for Object"  + objectCounter +" with Invocation:" + invocatioCter + ":Thread" + Thread.currentThread().getName());
        invocatioCter++;
        if(isDone)
            return;
        Buffer buffer = memoryManager.allocate(DEFAULT_BUFFER_SIZE);
        if(isFirst) {
            System.out.println("isFirst  release was - called from a second thread" + Thread.currentThread().getName());
            memoryManager.release(buffer);
            buffer = memoryManager.allocate(DEFAULT_BUFFER_SIZE);
            isFirst = false;
        }

        isDone = readStreamManually(buffer, invocatioCter);

        HttpContent content = httpResponsePacket.httpContentBuilder().content(buffer).build();
        ctx.getConnection().getAttributes().setAttribute(MULE_CLASSLOADER, loggerClassLoader);
        ctx.write(content, this);

        if (isDone) {
            content =  httpResponsePacket.httpTrailerBuilder().build();
            ctx.write(content, this);
        }
        System.out.println("Exiting sendInputStreamChunk for Object"  + objectCounter +" with Invocation:" + invocatioCter + ":Thread" + Thread.currentThread().getName());

    }

    private boolean readStreamManually(final Buffer buffer, int invocatioCter) throws IOException {
        /*
        System.out.println("Offset before:"
        + buffer.arrayOffset() + ":"
        + buffer.arrayOffset());
        buffer.clear();*/
        boolean isDone = false;
        byte[] bufferByteArray = buffer.array();
        System.out.println(String.format(" Input stream position before with invoctaion counter $ - %s",  inputStream.getPosition(), invocatioCter));
        //StringBuffer readDataBuf = new StringBuffer();
        int c;
        //int bytesRead = 0;
        int offset = buffer.arrayOffset();
        int length = buffer.remaining();
        System.out.println("Array len:" + bufferByteArray.length);
        System.out.println("ArrayOffset:" + buffer.arrayOffset());
        System.out.println("Position:" + buffer.position());
        System.out.println("Length:" + length);

        int current = 0;

        while ((c = inputStream.read()) != -1 && current < length) {
            bufferByteArray[offset++] = (byte) c;
            //readDataBuf.append((char)c);
            //readDataBuf.append((char)c);
            current++;
        }

        System.out.println("Buffer arrayOffet after write:" + buffer.arrayOffset() );
        System.out.println(" Input stream position After" + inputStream.getPosition());
        //System.out.println("Data inn String buffer" + readDataBuf.toString());
        System.out.println("Ending value of current" + current);

        if (c == -1)
            isDone = true;
        buffer.limit(current);
        //buffer.limit(current);
        return isDone;
    }

    private void writeWithLog(byte[] bufferByteArray) {
        System.out.println("About to write" + (new String(bufferByteArray)).toString());
    }

    /**
     * Method gets called, when file chunk was successfully sent.
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
                sendInputStreamChunk();
                // In HTTP 1.0 (no chunk supported) there is no more data sent to the client after the input stream is completed.
                // As there is no more data to be sent (in HTTP 1.1 a last chunk with '0' is sent) the #completed method is not called
                // So, we have to call it manually here
                if (isDone && !httpResponsePacket.isChunked())
                {
                    doComplete();
                }
            }
            else
            {
                doComplete();
            }
        }
        catch (IOException e)
        {
            failed(e);
        }
    }

    private void doComplete()
    {
        try {
            close();
            responseStatusCallback.responseSendSuccessfully();
            ctx.getConnection().getAttributes().removeAttribute(MULE_CLASSLOADER);
            ctx.notifyDownstream(RESPONSE_COMPLETE_EVENT);
            resume();
        } catch (Exception e) {

        }
    }

    /**
     * The method will be called, when file transferring was canceled
     */
    @Override
    public void cancelled()
    {
        super.cancelled();
        ctx.getConnection().getAttributes().removeAttribute(MULE_CLASSLOADER);
        close();
        responseStatusCallback.responseSendFailure(new DefaultMuleException(createStaticMessage("HTTP response sending task was cancelled")));
        resume();
    }

    /**
     * The method will be called, if file transferring was failed.
     *
     * @param throwable the cause
     */
    @Override
    public void failed(Throwable throwable)
    {
        super.failed(throwable);
        ctx.getConnection().getAttributes().removeAttribute(MULE_CLASSLOADER);
        close();
        resume();
    }

    /**
     * Close the local file input stream.
     */
    private void close()
    {
        try
        {
            inputStream.close();
        }
        catch (IOException e)
        {

        }
    }

    /**
     * Resume the HttpRequestPacket processing
     */
    private void resume()
    {
        ctx.resume(ctx.getStopAction());
    }
}
