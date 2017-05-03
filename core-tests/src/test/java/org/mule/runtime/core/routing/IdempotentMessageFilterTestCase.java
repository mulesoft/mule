/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class IdempotentMessageFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testIdempotentReceiver() throws Exception {
    IdempotentMessageFilter ir = new IdempotentMessageFilter();
    ir.setMuleContext(muleContext);
    ir.setStorePrefix("foo");
    ir.setObjectStore(new InMemoryObjectStore<String>());

    final EventContext contextA = mock(EventContext.class);
    when(contextA.getId()).thenReturn("1");

    Message okMessage = InternalMessage.builder().payload("OK").build();
    Event event = Event.builder(contextA).message(okMessage).build();

    // This one will process the event on the target endpoint
    Event processedEvent = ir.process(event);
    assertThat(processedEvent, not(nullValue()));
    verify(contextA, never()).success();
    verify(contextA, never()).success(any());
    verify(contextA, never()).error(any());

    final EventContext contextB = mock(EventContext.class);
    when(contextB.getId()).thenReturn("1");
    // This will not process, because the ID is a duplicate
    event = Event.builder(contextB).message(okMessage).build();
    processedEvent = ir.process(event);
    assertThat(processedEvent, nullValue());
    verify(contextB).success();
    verify(contextB, never()).success(any());
    verify(contextB, never()).error(any());
  }
}
