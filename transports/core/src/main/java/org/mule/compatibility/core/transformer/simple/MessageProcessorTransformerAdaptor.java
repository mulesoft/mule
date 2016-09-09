/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.processor.simple.SimpleMessageProcessor;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

/**
 * Provides a way to declare {@link SimpleMessageProcessor} instances as transformers inside an {@link InboundEndpoint} or
 * {@link OutboundEndpoint}.
 *
 * @since 4.0
 */
public class MessageProcessorTransformerAdaptor extends AbstractMessageTransformer implements MuleContextAware {

  private SimpleMessageProcessor messageProcessor;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    messageProcessor.setMuleContext(muleContext);
    messageProcessor.initialise();
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (event != null && event.getMessage() != null) {
      try {
        return messageProcessor.process(event);
      } catch (Exception e) {
        throw new MessageTransformerException(this, e);
      }
    }
    return event;
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    try {
      return messageProcessor.process(event).getMessage();
    } catch (MuleException e) {
      throw new TransformerException(this, e);
    }
  }


  public void setMessageProcessor(SimpleMessageProcessor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  public SimpleMessageProcessor getMessageProcessor() {
    return messageProcessor;
  }
}
