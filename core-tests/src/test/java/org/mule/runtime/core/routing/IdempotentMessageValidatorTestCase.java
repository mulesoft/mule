/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.routing.ValidationException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.routing.IdempotentMessageValidator;
import org.mule.tck.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IdempotentMessageValidatorTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = none();

  @Test
  public void idempotentReceiver() throws Exception {
    IdempotentMessageValidator idempotent = new IdempotentMessageValidator();
    idempotent.setMuleContext(muleContext);
    idempotent.setStorePrefix("foo");
    idempotent.setObjectStore(new InMemoryObjectStore<String>());

    final EventContext contextA = mock(EventContext.class);
    when(contextA.getCorrelationId()).thenReturn("1");

    Message okMessage = InternalMessage.builder().payload("OK").build();
    Event event = Event.builder(contextA).message(okMessage).build();

    // This one will process the event on the target endpoint
    Event processedEvent = idempotent.process(event);
    assertThat(processedEvent, sameInstance(event));

    final EventContext contextB = mock(EventContext.class);
    when(contextB.getCorrelationId()).thenReturn("1");

    // This will not process, because the ID is a duplicate
    event = Event.builder(contextB).message(okMessage).build();

    expected.expect(ValidationException.class);
    processedEvent = idempotent.process(event);
  }
}
