/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.AbstractExceptionListener;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

public class ExceptionListenerTestCase extends AbstractMuleTestCase {

  @Test
  public void testAddGoodEndpoint() throws Exception {
    AbstractExceptionListener router = new OnErrorPropagateHandler();
    Processor messageProcessor = Mockito.mock(Processor.class);
    router.addEndpoint(messageProcessor);
    assertNotNull(router.getMessageProcessors());
    assertTrue(router.getMessageProcessors().contains(messageProcessor));
  }

  @Test
  public void testSetGoodEndpoints() throws Exception {
    List<Processor> list = new ArrayList<Processor>();
    list.add(Mockito.mock(Processor.class));
    list.add(Mockito.mock(Processor.class));

    AbstractExceptionListener router = new OnErrorPropagateHandler();
    assertNotNull(router.getMessageProcessors());
    assertEquals(0, router.getMessageProcessors().size());

    router.addEndpoint(Mockito.mock(Processor.class));
    assertEquals(1, router.getMessageProcessors().size());

    router.setMessageProcessors(list);
    assertNotNull(router.getMessageProcessors());
    assertEquals(2, router.getMessageProcessors().size());
  }
}
