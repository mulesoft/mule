/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.grizzly;

import static org.glassfish.grizzly.http.HttpServerFilter.RESPONSE_COMPLETE_EVENT;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.response.HttpResponse;
import org.mule.service.http.api.server.async.ResponseStatusCallback;

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
 * {@link org.glassfish.grizzly.CompletionHandler}, responsible for asynchronous http response transferring when the response body
 * is an input stream.
 */
public class ResponseStreamingCompletionHandler extends BaseResponseCompletionHandler {

  private final MemoryManager memoryManager;
  private final FilterChainContext ctx;
  private final HttpResponsePacket httpResponsePacket;
  private final InputStream inputStream;
  private final ResponseStatusCallback responseStatusCallback;

  private volatile boolean isDone;

  public ResponseStreamingCompletionHandler(final FilterChainContext ctx, final HttpRequestPacket request,
                                            final HttpResponse httpResponse, ResponseStatusCallback responseStatusCallback) {
    Preconditions.checkArgument((httpResponse.getEntity() instanceof InputStreamHttpEntity),
                                "http response must have an input stream entity");
    this.ctx = ctx;
    httpResponsePacket = buildHttpResponsePacket(request, httpResponse);
    inputStream = ((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream();
    memoryManager = ctx.getConnection().getTransport().getMemoryManager();
    this.responseStatusCallback = responseStatusCallback;
  }

  public void start() throws IOException {
    sendInputStreamChunk();
  }

  public void sendInputStreamChunk() throws IOException {
    final Buffer buffer = memoryManager.allocate(8 * 1024);

    final byte[] bufferByteArray = buffer.array();
    final int offset = buffer.arrayOffset();
    final int length = buffer.remaining();

    int bytesRead = inputStream.read(bufferByteArray, offset, length);
    final HttpContent content;

    if (bytesRead == -1) {
      content = httpResponsePacket.httpTrailerBuilder().build();
      isDone = true;
    } else {
      buffer.limit(bytesRead);
      content = httpResponsePacket.httpContentBuilder().content(buffer).build();
    }

    ctx.write(content, this);
  }

  /**
   * Method gets called, when file chunk was successfully sent.
   *
   * @param result the result
   */
  @Override
  public void completed(WriteResult result) {
    try {
      if (!isDone) {
        sendInputStreamChunk();
        // In HTTP 1.0 (no chunk supported) there is no more data sent to the client after the input stream is completed.
        // As there is no more data to be sent (in HTTP 1.1 a last chunk with '0' is sent) the #completed method is not called
        // So, we have to call it manually here
        if (isDone && !httpResponsePacket.isChunked()) {
          doComplete();
        }
      } else {
        doComplete();
      }
    } catch (IOException e) {
      failed(e);
    }
  }

  private void doComplete() {
    close();
    responseStatusCallback.responseSendSuccessfully();
    ctx.notifyDownstream(RESPONSE_COMPLETE_EVENT);
    resume();
  }

  /**
   * The method will be called, when file transferring was canceled
   */
  @Override
  public void cancelled() {
    close();
    responseStatusCallback.responseSendFailure(new DefaultMuleException(CoreMessages
        .createStaticMessage("Http response sending task was cancelled")));
    resume();
  }

  /**
   * The method will be called, if file transferring was failed.
   *
   * @param throwable the cause
   */
  @Override
  public void failed(Throwable throwable) {
    close();
    responseStatusCallback.responseSendFailure(throwable);
    resume();
  }

  /**
   * Close the local file input stream.
   */
  private void close() {
    try {
      inputStream.close();
    } catch (IOException e) {

    }
  }

  /**
   * Resume the HttpRequestPacket processing
   */
  private void resume() {
    ctx.resume(ctx.getStopAction());
  }
}
