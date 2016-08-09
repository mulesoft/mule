/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;

import org.junit.Test;

public class ResponseMessageProcessorAdapterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testResponseAdaptorSingleMP() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(createStringAppendTransformer("1"), new ResponseMessageProcessorAdapter(createStringAppendTransformer("3")),
                  createStringAppendTransformer("2"));
    assertEquals("0123", builder.build().process(getTestEvent("0")).getMessageAsString());
  }

  @Test
  public void testResponseAdaptorSingleMPReturnsNull() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(createStringAppendTransformer("1"), new ResponseMessageProcessorAdapter(new ReturnNullMP()),
                  createStringAppendTransformer("2"));
    assertEquals("012", builder.build().process(getTestEvent("0")).getMessageAsString());
  }

  @Test
  public void testResponseAdaptorNestedChain() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(createStringAppendTransformer("1"),
                  new ResponseMessageProcessorAdapter(new DefaultMessageProcessorChainBuilder()
                      .chain(createStringAppendTransformer("a"), createStringAppendTransformer("b")).build()),
                  createStringAppendTransformer("2"));
    assertEquals("012ab", builder.build().process(getTestEvent("0")).getMessageAsString());
  }

  @Test
  public void testResponseAdaptorNestedChainReturnsNull() throws MuleException, Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(createStringAppendTransformer("1"),
                  new ResponseMessageProcessorAdapter(new DefaultMessageProcessorChainBuilder()
                      .chain(createStringAppendTransformer("a"), createStringAppendTransformer("b"), new ReturnNullMP()).build()),
                  createStringAppendTransformer("2"));
    assertEquals("012", builder.build().process(getTestEvent("0")).getMessageAsString());
  }

  private StringAppendTransformer createStringAppendTransformer(String append) {
    StringAppendTransformer transformer = new StringAppendTransformer(append);
    transformer.setMuleContext(muleContext);
    return transformer;
  }

  private static class ReturnNullMP implements MessageProcessor {

    public MuleEvent process(MuleEvent event) throws MuleException {
      return null;
    }
  }

}
