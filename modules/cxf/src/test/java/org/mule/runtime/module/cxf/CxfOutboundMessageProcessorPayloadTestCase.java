/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class CxfOutboundMessageProcessorPayloadTestCase extends AbstractMuleContextTestCase {

  private CxfOutboundMessageProcessor cxfMP;
  private CxfConfiguration configuration;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    configuration = new CxfConfiguration();
    configuration.setMuleContext(muleContext);
    configuration.initialise();
    cxfMP = new CxfOutboundMessageProcessor(null);
  }

  @Test
  public void testGetArgs_withObjectAsPayload() throws Exception {
    Object payload = new Object();

    Object[] args = callGetArgsWithPayload(payload);

    assertNotNull(args);
    assertEquals(1, args.length);
    assertSame(payload, args[0]);
  }

  @Test
  public void testGetArgs_withArrayAsPayload() throws Exception {
    Object[] payload = new Object[4];

    Object[] args = callGetArgsWithPayload(payload);

    assertSame(payload, args);
  }

  @Test
  public void testGetArgs_withNullPayloadAsPayload() throws Exception {
    Object payload = null;

    Object[] args = callGetArgsWithPayload(payload);

    assertNotNull(args);
    assertEquals(1, args.length);
    assertSame(payload, args[0]);
  }

  private Object[] callGetArgsWithPayload(Object payload) throws TransformerException {
    MuleEvent muleEvent = mock(MuleEvent.class);
    MuleMessage muleMessage = mock(MuleMessage.class);

    when(muleEvent.getMessage()).thenReturn(muleMessage);
    when(muleEvent.getMessage().getPayload()).thenReturn(payload);
    when(muleMessage.getPayload()).thenReturn(payload);

    Object[] args = cxfMP.getArgs(muleEvent);
    return args;
  }

}

