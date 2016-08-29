/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.tck.processor.TestNonBlockingProcessor;

public class TestMessageDispatcher extends AbstractMessageDispatcher {

  public TestMessageDispatcher(final OutboundEndpoint endpoint) {
    super(endpoint);
  }

  private TestNonBlockingProcessor nonBlockingProcessor = new TestNonBlockingProcessor();

  @Override
  protected void doInitialise() {
    try {
      nonBlockingProcessor.initialise();
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected void doDispose() {
    nonBlockingProcessor.dispose();
  }

  @Override
  protected void doDispatch(MuleEvent event) throws Exception {
    if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail")) {
      throw new RoutingException(event, (OutboundEndpoint) endpoint);
    }
  }

  @Override
  protected MuleMessage doSend(MuleEvent event) throws Exception {
    if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail")) {
      throw new RoutingException(event, (OutboundEndpoint) endpoint);
    }
    return event.getMessage();
  }

  @Override
  protected void doSendNonBlocking(MuleEvent event, final CompletionHandler<MuleMessage, Exception, Void> completionHandler) {
    if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail")) {
      completionHandler.onFailure(new RoutingException(event, (OutboundEndpoint) endpoint));
    } else {
      try {
        final MuleMessage response = event.getMessage();
        event = MuleEvent.builder(event).replyToHandler(new ReplyToHandler() {

          @Override
          public MuleEvent processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) {
            completionHandler.onCompletion(response, (ExceptionCallback<Void, Exception>) (exception -> {
              // TODO MULE-9629
              return null;
            }));
            return event;
          }

          @Override
          public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
            completionHandler.onFailure(exception);
          }
        }).build();
        nonBlockingProcessor.process(event);
      } catch (Exception e) {
        completionHandler.onFailure(e);
      }
    }
  }

  @Override
  protected void doConnect() throws Exception {
    // no op
  }

  @Override
  protected void doDisconnect() throws Exception {
    // no op
  }

  @Override
  protected void doStart() {
    // no op
  }

  @Override
  protected void doStop() {
    // no op
  }

  @Override
  protected boolean isSupportsNonBlocking() {
    return true;
  }
}
