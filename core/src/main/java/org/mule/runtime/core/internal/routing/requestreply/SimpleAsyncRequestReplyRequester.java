/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.requestreply;

import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.connector.DefaultReplyToHandler;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.privileged.endpoint.LegacyImmutableEndpoint;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.privileged.transport.LegacyInboundEndpoint;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.routing.requestreply.AbstractReplyToPropertyRequestReplyReplier;

public class SimpleAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester implements Startable, Stoppable {

  protected Processor requestMessageProcessor;

  @Override
  protected void sendAsyncRequest(CoreEvent event) throws MuleException {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(MULE_REPLY_TO_PROPERTY, getReplyTo())
            .addOutboundProperty(MULE_REPLY_TO_REQUESTOR_PROPERTY, getLocation().getRootContainerName())
            .build())
        .build();
    requestMessageProcessor.process(event);
  }

  protected String getReplyTo() {
    // TODO See MULE-9307 - re-add logic to get reply to destination for request-reply
    return replyMessageSource instanceof LegacyInboundEndpoint ? ((LegacyInboundEndpoint) replyMessageSource).getCanonicalURI()
        : null;
  }

  public void setMessageProcessor(Processor processor) {
    requestMessageProcessor = processor;
  }

  @Deprecated
  public void setMessageSource(MessageSource source) {
    setReplySource(source);
  }

  @Override
  public void start() throws MuleException {
    if (replyMessageSource != null) {
      if (replyMessageSource instanceof Initialisable) {
        ((Initialisable) replyMessageSource).initialise();
      }
      if (replyMessageSource instanceof Startable) {
        ((Startable) replyMessageSource).start();
      }
    }
    if (requestMessageProcessor != null) {
      if (requestMessageProcessor instanceof Initialisable) {
        ((Initialisable) requestMessageProcessor).initialise();
      }
      if (requestMessageProcessor instanceof Startable) {
        ((Startable) requestMessageProcessor).start();
      }
    }
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    if (replyMessageSource != null && replyMessageSource instanceof Stoppable) {
      ((Stoppable) replyMessageSource).stop();

      if (requestMessageProcessor != null && requestMessageProcessor instanceof Stoppable) {
        ((Stoppable) requestMessageProcessor).stop();
      }
    }
    if (requestMessageProcessor != null) {
      if (requestMessageProcessor instanceof Stoppable) {
        ((Stoppable) requestMessageProcessor).stop();
      }
      if (requestMessageProcessor instanceof Disposable) {
        ((Disposable) requestMessageProcessor).dispose();
      }
    }
    super.stop();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    if (requestMessageProcessor instanceof MuleContextAware) {
      ((MuleContextAware) requestMessageProcessor).setMuleContext(context);
    }
  }

  public static class AsyncReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier {

    private final MessageSource source;

    public AsyncReplyToPropertyRequestReplyReplier(MessageSource source) {
      this.source = source;
    }

    @Override
    protected boolean shouldProcessEvent(PrivilegedEvent event) {
      // Only process ReplyToHandler is running one-way and standard ReplyToHandler is being used.
      MessageExchangePattern mep = REQUEST_RESPONSE;
      if (source instanceof LegacyImmutableEndpoint) {
        mep = ((LegacyImmutableEndpoint) source).getExchangePattern();
      }
      return !mep.hasResponse() && event.getReplyToHandler() instanceof DefaultReplyToHandler;
    }

  }
}
