/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.internal.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.el.ExpressionLanguageAdaptor;
import org.mule.runtime.core.api.routing.ValidationException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.el.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IdempotentMessageValidatorTestCase extends AbstractMuleContextTestCase {

  private static IdempotentMessageValidator idempotent;

  @Before
  public void reset() {
    //Needs to create a new validator for every test because the idExpression needs to be reset and there is not way of knowing the default from the test
    idempotent = new IdempotentMessageValidator();
    idempotent.setStorePrefix("foo");
    idempotent.setObjectStore(new InMemoryObjectStore<String>());
    idempotent.setMuleContext(muleContext);
  }

  @Rule
  public ExpectedException expected = none();

  @Test
  public void idempotentReceiver() throws Exception {

    final EventContext contextA = mock(EventContext.class);
    when(contextA.getCorrelationId()).thenReturn("1");

    Message okMessage = InternalMessage.builder().value("OK").build();
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

  @Test
  public void testIdCheckWithMEL() throws Exception {
    String melExpression = "#[payload]";
    final EventContext context = mock(EventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message okMessage = of("OK");
    Event event = Event.builder(context).message(okMessage).build();

    //Set MEL expression to hash value
    idempotent.setIdExpression(melExpression);

    // This one will process the event on the target endpoint
    Event processedEvent = idempotent.process(event);
    assertNotNull(processedEvent);
    assertEquals(idempotent.getObjectStore().retrieve("OK"), "1");

    // This will not process, because the message is a duplicate
    okMessage = of("OK");
    event = Event.builder(context).message(okMessage).build();

    expected.expect(ValidationException.class);
    processedEvent = idempotent.process(event);
  }

  @Test
  public void testIdCheckWithDW() throws Exception {
    String dwExpression = "%dw 2.0\n" +
        "output application/text\n" +
        "---\n" +
        "payload ++ ' World'";
    final EventContext context = mock(EventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message okMessage = of("Hello");
    Event event = Event.builder(context).message(okMessage).build();

    //Set DW expression to hash value
    idempotent.setIdExpression(dwExpression);

    // This one will process the event on the target endpoint
    Event processedEvent = idempotent.process(event);
    assertNotNull(processedEvent);
    assertEquals(idempotent.getObjectStore().retrieve("Hello World"), "1");

    // This will not process, because the message is a duplicate
    okMessage = of("Hello");
    event = Event.builder(context).message(okMessage).build();

    expected.expect(ValidationException.class);
    processedEvent = idempotent.process(event);
  }

  @Test
  public void testIdCheckWithHash() throws Exception {
    String dwHashExpression = "%dw 2.0\n" +
        "output text/plain\n" +
        "import dw::Crypto\n" +
        "---\n" +
        "Crypto::hashWith(payload,'SHA-256')";
    String payload = "payload to be hashed";
    final EventContext context = mock(EventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message message = of(payload);
    Event event = Event.builder(context).message(message).build();

    //Set DW expression to hash value
    idempotent.setIdExpression(dwHashExpression);

    //Evaluate DW expression outside MessageValidator
    ExpressionLanguageAdaptor expressionLanguageAdaptor =
        new DataWeaveExpressionLanguageAdaptor(muleContext, new WeaveDefaultExpressionLanguageFactoryService());
    TypedValue hashedValue = expressionLanguageAdaptor.evaluate(dwHashExpression, event, NULL_BINDING_CONTEXT);

    // This one will process the event on the target endpoint
    Event processedEvent = idempotent.process(event);
    assertNotNull(processedEvent);
    assertEquals(idempotent.getObjectStore()
        .retrieve(IOUtils.toString((ByteArrayBasedCursorStreamProvider) hashedValue.getValue())), "1");

    // This will not process, because the message is a duplicate
    message = of(payload);
    event = Event.builder(context).message(message).build();

    expected.expect(ValidationException.class);
    processedEvent = idempotent.process(event);
  }

  @Test
  public void differentIdsShouldBeStored() throws Exception {
    String dwHashExpression = "%dw 2.0\n" +
        "output text/plain\n" +
        "import dw::Crypto\n" +
        "---\n" +
        "Crypto::SHA1(payload)";
    String payload = "payload to be hashed";
    String otherPayload = "this is another payload to be hashed";
    final EventContext context = mock(EventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message message = of(payload);
    Event event = Event.builder(context).message(message).build();

    //Set DW expression to hash value
    idempotent.setIdExpression(dwHashExpression);

    //Evaluate DW expression outside MessageValidator
    ExpressionLanguageAdaptor expressionLanguageAdaptor =
        new DataWeaveExpressionLanguageAdaptor(muleContext, new WeaveDefaultExpressionLanguageFactoryService());
    TypedValue hashedValue = expressionLanguageAdaptor.evaluate(dwHashExpression, event, NULL_BINDING_CONTEXT);

    // This one will process the event on the target endpoint
    Event processedEvent = idempotent.process(event);
    assertNotNull(processedEvent);
    assertEquals(idempotent.getObjectStore()
        .retrieve(IOUtils.toString((ByteArrayBasedCursorStreamProvider) hashedValue.getValue())), "1");

    // This will process, because the message is a new one
    Message otherMessage = of(otherPayload);
    event = Event.builder(context).message(otherMessage).build();

    processedEvent = idempotent.process(event);
    assertNotNull(processedEvent);
  }
}
