/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.IdempotentMessageValidator.IDEMPOTENT_MESSAGE_VALIDATOR;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.el.ExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.exception.ValidationException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.internal.routing.split.DuplicateMessageException;

import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CORE_COMPONENTS)
@Story(IDEMPOTENT_MESSAGE_VALIDATOR)
public class IdempotentMessageValidatorTestCase extends AbstractMuleContextTestCase {

  private static IdempotentMessageValidator idempotent;

  @Before
  public void reset() {
    // Needs to create a new validator for every test because the idExpression needs to be reset and there is not way of knowing
    // the default from the test
    idempotent = new IdempotentMessageValidator();
    idempotent.setStorePrefix("foo");
    idempotent.setObjectStore(new InMemoryObjectStore<>());
    idempotent.setMuleContext(muleContext);
  }

  @After
  public void after() throws Exception {
    idempotent.stop();
    idempotent.dispose();
  }

  @Rule
  public ExpectedException expected = none();

  @Test
  public void idempotentReceiver() throws Exception {

    final BaseEventContext contextA = mock(BaseEventContext.class);
    when(contextA.getCorrelationId()).thenReturn("1");

    Message okMessage = InternalMessage.builder().value("OK").build();
    CoreEvent event = CoreEvent.builder(contextA).message(okMessage).build();

    initialiseIfNeeded(idempotent, true, muleContext);
    // This one will process the event on the target endpoint
    CoreEvent processedEvent = idempotent.process(event);
    assertThat(processedEvent, sameInstance(event));

    final BaseEventContext contextB = mock(BaseEventContext.class);
    when(contextB.getCorrelationId()).thenReturn("1");

    // This will not process, because the ID is a duplicate
    event = CoreEvent.builder(contextB).message(okMessage).build();

    expected.expect(ValidationException.class);
    processedEvent = idempotent.process(event);
  }

  @Test
  public void testIdCheckWithMEL() throws Exception {
    String melExpression = "#[payload]";
    final BaseEventContext context = mock(BaseEventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message okMessage = of("OK");
    CoreEvent event = CoreEvent.builder(context).message(okMessage).build();

    // Set MEL expression to hash value
    idempotent.setIdExpression(melExpression);

    initialiseIfNeeded(idempotent, true, muleContext);
    // This one will process the event on the target endpoint
    CoreEvent processedEvent = idempotent.process(event);
    assertThat(processedEvent, is(notNullValue()));
    assertThat(idempotent.getObjectStore().retrieve("OK"), is("1"));

    // This will not process, because the message is a duplicate
    okMessage = of("OK");
    event = CoreEvent.builder(context).message(okMessage).build();

    expected.expect(ValidationException.class);
    processedEvent = idempotent.process(event);
  }

  @Test
  public void testIdCheckWithDW() throws Exception {
    String dwExpression = "%dw 2.0\n" +
        "output application/text\n" +
        "---\n" +
        "payload ++ ' World'";
    final BaseEventContext context = mock(BaseEventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message okMessage = of("Hello");
    CoreEvent event = CoreEvent.builder(context).message(okMessage).build();

    // Set DW expression to hash value
    idempotent.setIdExpression(dwExpression);

    initialiseIfNeeded(idempotent, true, muleContext);
    // This one will process the event on the target endpoint
    CoreEvent processedEvent = idempotent.process(event);
    assertThat(processedEvent, is(notNullValue()));
    assertThat(idempotent.getObjectStore().retrieve("Hello World"), is("1"));

    // This will not process, because the message is a duplicate
    okMessage = of("Hello");
    event = CoreEvent.builder(context).message(okMessage).build();

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
    final BaseEventContext context = mock(BaseEventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message message = of(payload);
    CoreEvent event = CoreEvent.builder(context).message(message).build();

    // Set DW expression to hash value
    idempotent.setIdExpression(dwHashExpression);

    // Evaluate DW expression outside MessageValidator
    ExpressionLanguageAdaptor expressionLanguageAdaptor =
        new DataWeaveExpressionLanguageAdaptor(muleContext, mock(Registry.class),
                                               new WeaveDefaultExpressionLanguageFactoryService(null),
                                               getFeatureFlaggingService());
    TypedValue<?> hashedValue = expressionLanguageAdaptor.evaluate(dwHashExpression, event, NULL_BINDING_CONTEXT);

    initialiseIfNeeded(idempotent, true, muleContext);
    // This one will process the event on the target endpoint
    CoreEvent processedEvent = idempotent.process(event);
    assertThat(processedEvent, is(notNullValue()));
    assertThat(idempotent.getObjectStore()
        .retrieve(IOUtils.toString((ByteArrayBasedCursorStreamProvider) hashedValue.getValue())), is("1"));

    // This will not process, because the message is a duplicate
    message = of(payload);
    event = CoreEvent.builder(context).message(message).build();

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
    final BaseEventContext context = mock(BaseEventContext.class);
    when(context.getCorrelationId()).thenReturn("1");
    Message message = of(payload);
    CoreEvent event = CoreEvent.builder(context).message(message).build();

    // Set DW expression to hash value
    idempotent.setIdExpression(dwHashExpression);

    // Evaluate DW expression outside MessageValidator
    ExpressionLanguageAdaptor expressionLanguageAdaptor =
        new DataWeaveExpressionLanguageAdaptor(muleContext, mock(Registry.class),
                                               new WeaveDefaultExpressionLanguageFactoryService(null),
                                               getFeatureFlaggingService());
    TypedValue<Object> hashedValue = expressionLanguageAdaptor.evaluate(dwHashExpression, event, NULL_BINDING_CONTEXT);

    initialiseIfNeeded(idempotent, true, muleContext);
    // This one will process the event on the target endpoint
    CoreEvent processedEvent = idempotent.process(event);
    assertThat(processedEvent, is(notNullValue()));
    assertThat(idempotent.getObjectStore()
        .retrieve(IOUtils.toString((ByteArrayBasedCursorStreamProvider) hashedValue.getValue())), is("1"));

    // This will process, because the message is a new one
    Message otherMessage = of(otherPayload);
    event = CoreEvent.builder(context).message(otherMessage).build();

    processedEvent = idempotent.process(event);
    assertThat(processedEvent, is(notNullValue()));
  }

  @Test
  public void multipleObjectStoreConfigurationShouldRaiseException() throws Exception {
    idempotent.setPrivateObjectStore(new InMemoryObjectStore<>());
    expected.expect(InitialisationException.class);
    idempotent.initialise();
  }

  @Test
  @Issue("W-11529823")
  public void rethrowsException() throws Exception {
    final BaseEventContext context = mock(BaseEventContext.class);
    when(context.getCorrelationId()).thenReturn("1");

    ObjectStore<String> objectStore = mock(ObjectStore.class);
    doThrow(ObjectStoreException.class).when(objectStore).store(anyString(), any());
    idempotent.setObjectStore(objectStore);

    Message okMessage = InternalMessage.builder().value("OK").build();
    CoreEvent event = CoreEvent.builder(context).message(okMessage).build();

    initialiseIfNeeded(idempotent, true, muleContext);
    // set rethrow as if FF is enabled
    idempotent.setRethrowEnabled(true);
    expected.expect(ObjectStoreException.class);
    idempotent.process(event);
  }

  @Test
  @Issue("W-11529823")
  public void throwsDuplicateMessageException() throws Exception {
    final BaseEventContext contextA = mock(BaseEventContext.class);
    when(contextA.getCorrelationId()).thenReturn("1");

    ObjectStore<String> objectStore = mock(ObjectStore.class);
    doThrow(ObjectStoreException.class).when(objectStore).store(anyString(), any());
    idempotent.setObjectStore(objectStore);

    Message okMessage = InternalMessage.builder().value("OK").build();
    CoreEvent event = CoreEvent.builder(contextA).message(okMessage).build();

    initialiseIfNeeded(idempotent, true, muleContext);
    // set rethrow as if FF is disabled
    idempotent.setRethrowEnabled(false);
    expected.expect(DuplicateMessageException.class);
    idempotent.process(event);
  }

  @Test
  public void implicitObjectStoreIsCreatedWhenNonDefined() throws Exception {
    idempotent.setObjectStore(null);
    initialiseIfNeeded(idempotent, true, muleContext);
    assertThat(idempotent.getObjectStore(), is(notNullValue()));
  }


}
