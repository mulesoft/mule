/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.requestreply;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transport.LegacyInboundEndpoint;

public class SimpleAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester implements Startable, Stoppable {

  protected MessageProcessor requestMessageProcessor;

  @Override
  protected void sendAsyncRequest(MuleEvent event) throws MuleException {
    setAsyncReplyProperties(event);
    requestMessageProcessor.process(event);
  }

  protected void setAsyncReplyProperties(MuleEvent event) throws MuleException {
    event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty(MULE_REPLY_TO_PROPERTY, getReplyTo())
        .addOutboundProperty(MULE_REPLY_TO_REQUESTOR_PROPERTY, event.getFlowConstruct().getName()).build());
  }

  protected String getReplyTo() {
    // TODO See MULE-9307 - re-add logic to get reply to destination for request-reply
    return replyMessageSource instanceof LegacyInboundEndpoint ? ((LegacyInboundEndpoint) replyMessageSource).getCanonicalURI()
        : null;
  }

  public void setMessageProcessor(MessageProcessor processor) {
    requestMessageProcessor = processor;
  }

  @Deprecated
  public void setMessageSource(MessageSource source) {
    setReplySource(source);
  }

  @Override
  public void start() throws MuleException {
    if (replyMessageSource != null) {
      if (replyMessageSource instanceof FlowConstructAware) {
        ((FlowConstructAware) replyMessageSource).setFlowConstruct(this.flowConstruct);
      }
      if (replyMessageSource instanceof Initialisable) {
        ((Initialisable) replyMessageSource).initialise();
      }
      if (replyMessageSource instanceof Startable) {
        ((Startable) replyMessageSource).start();
      }
    }
    if (requestMessageProcessor != null) {
      if (requestMessageProcessor instanceof FlowConstructAware) {
        ((FlowConstructAware) requestMessageProcessor).setFlowConstruct(this.flowConstruct);
      }
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

}
