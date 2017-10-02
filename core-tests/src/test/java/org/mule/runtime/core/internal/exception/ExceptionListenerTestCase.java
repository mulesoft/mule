/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ExceptionListenerTestCase extends AbstractMuleTestCase {

  @Test
  public void setSingleGoodProcessorEndpoint() throws Exception {
    AbstractExceptionListener router = new OnErrorPropagateHandler();
    Processor messageProcessor = mock(Processor.class);
    router.setMessageProcessors(singletonList(messageProcessor));
    assertNotNull(router.getMessageProcessors());
    assertTrue(router.getMessageProcessors().contains(messageProcessor));
  }

  @Test
  public void setGoodProcessors() throws Exception {
    List<Processor> list = new ArrayList<Processor>();
    list.add(mock(Processor.class));
    list.add(mock(Processor.class));

    AbstractExceptionListener router = new OnErrorPropagateHandler();
    assertNotNull(router.getMessageProcessors());
    assertEquals(0, router.getMessageProcessors().size());

    router.setMessageProcessors(singletonList(mock(Processor.class)));
    assertEquals(1, router.getMessageProcessors().size());

    router.setMessageProcessors(list);
    assertNotNull(router.getMessageProcessors());
    assertEquals(2, router.getMessageProcessors().size());
  }
}
