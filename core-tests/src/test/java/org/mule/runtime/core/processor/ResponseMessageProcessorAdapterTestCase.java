/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;

import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ResponseMessageProcessorAdapterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testResponseAdaptorSingleMP() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(createStringAppendTransformer("3"));
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(createStringAppendTransformer("1"), responseMessageProcessorAdapter, createStringAppendTransformer("2"));
    assertEquals("0123", process(builder.build(), eventBuilder().message(InternalMessage.of("0")).build())
        .getMessageAsString(muleContext));
  }

  @Test
  public void testResponseAdaptorSingleMPReturnsNull() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(new ReturnNullMP());
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(createStringAppendTransformer("1"), responseMessageProcessorAdapter, createStringAppendTransformer("2"));
    assertEquals("012", process(builder.build(), eventBuilder()
        .message(InternalMessage.of("0"))
        .build()).getMessageAsString(muleContext));
  }

  @Test
  public void testResponseAdaptorNestedChain() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(new DefaultMessageProcessorChainBuilder()
            .chain(createStringAppendTransformer("a"), createStringAppendTransformer("b")).build());
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(createStringAppendTransformer("1"), responseMessageProcessorAdapter, createStringAppendTransformer("2"));
    assertEquals("012ab", process(builder.build(), eventBuilder().message(InternalMessage.of("0")).build())
        .getMessageAsString(muleContext));
  }

  @Test
  public void testResponseAdaptorNestedChainReturnsNull() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(new DefaultMessageProcessorChainBuilder()
            .chain(createStringAppendTransformer("a"), createStringAppendTransformer("b"), new ReturnNullMP()).build());
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(createStringAppendTransformer("1"), responseMessageProcessorAdapter, createStringAppendTransformer("2"));
    assertEquals("012", process(builder.build(), eventBuilder().message(InternalMessage.of("0")).build())
        .getMessageAsString(muleContext));
  }

  private StringAppendTransformer createStringAppendTransformer(String append) {
    StringAppendTransformer transformer = new StringAppendTransformer(append);
    transformer.setMuleContext(muleContext);
    return transformer;
  }

  private static class ReturnNullMP implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return null;
    }
  }

}
