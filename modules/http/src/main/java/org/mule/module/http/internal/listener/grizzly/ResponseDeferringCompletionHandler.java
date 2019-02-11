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
import org.mule.api.transport.OutputHandler;
import org.mule.module.http.internal.domain.OutputHandlerHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * {@link org.glassfish.grizzly.CompletionHandler}, responsible for asynchronous http response transferring
 * when the response body is an {@link OutputHandler}.
 *
 * @since 3.10
 */
public class ResponseDeferringCompletionHandler extends BaseResponseCompletionHandler
{
  private final MemoryManager memoryManager;
  private final HttpResponsePacket httpResponsePacket;
  private final OutputHandler outputHandler;
  private final ResponseStatusCallback responseStatusCallback;
  private final CompletionOutputStream outputStream;
  private final Semaphore sending = new Semaphore(1);

  private volatile boolean isDone;

  public ResponseDeferringCompletionHandler(final FilterChainContext ctx,
                                            final HttpRequestPacket request, final HttpResponse httpResponse, ResponseStatusCallback responseStatusCallback)
  {
    super(ctx);
    Preconditions.checkArgument((httpResponse.getEntity() instanceof OutputHandlerHttpEntity), "http response must have an output handler entity");
    httpResponsePacket = buildHttpResponsePacket(request, httpResponse);
    outputHandler = ((OutputHandlerHttpEntity) httpResponse.getEntity()).getOutputHandler();
    memoryManager = ctx.getConnection().getTransport().getMemoryManager();
    this.responseStatusCallback = responseStatusCallback;
    outputStream = new CompletionOutputStream(this);
  }

    @Override
    protected void doStart() throws IOException
    {
        try
        {
            outputHandler.write(null, outputStream);
        }
        catch (IOException e)
        {
            // Check whether a 200 has already gone through
            if (outputStream.isWritten())
            {
                logger.warn("Failure while processing HTTP response body. Cancelling.", e);
                outputStream.close();
            }
            else
            {
                throw e;
            }
        }
    }

  /**
   * Method gets called, when file chunk was successfully sent.
   *
   * @param result the result
   */
  @Override
  public void completed(WriteResult result)
  {
    if (isDone)
    {
      doComplete();
    }
    // Allow more data to be sent
    sending.release();
  }

  private void doComplete()
  {
    responseStatusCallback.responseSendSuccessfully();
    ctx.notifyDownstream(RESPONSE_COMPLETE_EVENT);
    resume();
  }

  /**
   * The method will be called, when file transferring was canceled
   */
  @Override
  public void cancelled()
  {
    super.cancelled();
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
    resume();
  }

  /**
   * Resume the HttpRequestPacket processing
   */
  private void resume()
  {
    ctx.resume(ctx.getStopAction());
  }

  class CompletionOutputStream extends OutputStream
  {

    private Buffer buffer = getBuffer(BaseResponseCompletionHandler.DEFAULT_BUFFER_SIZE);
    private boolean written = false;
    private CompletionHandler completionHandler;

    CompletionOutputStream(CompletionHandler completionHandler)
    {
      this.completionHandler = completionHandler;
    }

    @Override
    public void write(int b) throws IOException
    {
      flushIfNecessary(1);
      buffer.put((byte) b);
      buffer.limit(buffer.position() + 1);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
      write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len)
    {
      flushIfNecessary(len);
      buffer.put(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
      flush(DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void close() throws IOException
    {
      if (!isDone)
      {
        HttpContent content = httpResponsePacket.httpTrailerBuilder().build();
        if (hasPendingData())
        {
          content = getBufferAsContent().append(content);
        }
        try
        {
          sending.acquire();
          isDone = true;
          ctx.write(content, completionHandler);
          written = true;
          if (!httpResponsePacket.isChunked())
          {
            // In HTTP 1.0 (no chunk supported) there is no more data sending to the client after the input stream is completed.
            // As there is no more data to be sending (in HTTP 1.1 a last chunk with '0' is sending) the #completed method is not called
            // So, we have to call it manually here
            sending.release();
            doComplete();
          }
        }
        catch (InterruptedException e)
        {
          Thread.currentThread().interrupt();
        }
      }
    }

    public boolean isWritten()
    {
      return written;
    }

    /**
     * Checks whether there's enough space for the data, flushing and renewing the current buffer if not.
     *
     * @param writeLength the amount of data intended for the current buffer
     */
    private void flushIfNecessary(int writeLength)
    {
      if (buffer.remaining() < writeLength)
      {
        // The data is already in-memory, so send it all
        flush(Math.max(writeLength, DEFAULT_BUFFER_SIZE));
      }
    }

    /**
     * Sends all pending data (if any) and renews the current buffer.
     *
     * @param bufferSize size to renew the buffer after flushing
     */
    public void flush(int bufferSize)
    {
      if (hasPendingData())
      {
        try
        {
          sending.acquire();
          ctx.write(getBufferAsContent(), completionHandler);
          written = true;
        }
        catch (InterruptedException e)
        {
          Thread.currentThread().interrupt();
        }
      }
      else
      {
        // There's no data, we just needed more space
        buffer.release();
      }
      buffer = getBuffer(bufferSize);
    }

    private boolean hasPendingData()
    {
      return buffer.capacity() != buffer.remaining();
    }

    private HttpContent getBufferAsContent()
    {
      // Prepare buffer for reading
      buffer.flip();
      return httpResponsePacket.httpContentBuilder().content(buffer).build();
    }

    /**
     * @param bufferSize size of the buffer to create.
     * @return a new {@link Buffer}.
     */
    private Buffer getBuffer(int bufferSize)
    {
      return memoryManager.allocate(bufferSize);
    }
  }
}
