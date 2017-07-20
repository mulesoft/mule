/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.routing.ValidationException;
import org.mule.runtime.core.internal.routing.IdempotentSecureHashMessageValidator;
import org.mule.tck.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IdempotentSecureHashMessageValidatorTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = none();

  private IdempotentSecureHashMessageValidator secureHash;
  private EventContext context;

  public IdempotentSecureHashMessageValidatorTestCase() {
    setStartContext(true);
  }

  @Before
  public void before() throws MuleException {
    Flow flow = getTestFlow(muleContext);

    secureHash = new IdempotentSecureHashMessageValidator();
    secureHash.setFlowConstruct(flow);
    secureHash.setStorePrefix("foo");
    secureHash.setObjectStore(new InMemoryObjectStore<>());
    secureHash.setMuleContext(muleContext);

    context = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION);
  }

  @Test
  public void secureHashFiltered() throws Exception {
    Message okMessage = of("OK");
    Event event = Event.builder(context).message(okMessage).build();

    // This one will process the event on the target endpoint
    Event processedEvent = secureHash.process(event);
    assertNotNull(processedEvent);

    // This will not process, because the message is a duplicate
    okMessage = of("OK");
    event = Event.builder(context).message(okMessage).build();

    expected.expect(ValidationException.class);
    processedEvent = secureHash.process(event);
  }

  @Test
  public void secureHashNotFiltered() throws Exception {
    Message okMessage = of("OK");
    Event event = Event.builder(context).message(okMessage).build();

    // This one will process the event on the target endpoint
    Event processedEvent = secureHash.process(event);
    assertNotNull(processedEvent);

    // This will process, because the message is not a duplicate
    okMessage = of("Not OK");
    event = Event.builder(context).message(okMessage).build();
    processedEvent = secureHash.process(event);
    assertNotNull(processedEvent);
  }
}
