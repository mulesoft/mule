/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.privileged.processor.IdempotentRedeliveryPolicy.SECURE_HASH_EXPR_FORMAT;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.processor.IdempotentRedeliveryPolicy.RedeliveryCounter;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Mono;

public class IdempotentRedeliveryPolicyTestCase extends AbstractMuleTestCase {

  public static final String STRING_MESSAGE = "message";
  public static final int MAX_REDELIVERY_COUNT = 5;
  private static ObjectSerializer serializer;

  private MuleContextWithRegistries mockMuleContext = mockMuleContext();
  private ObjectStoreManager mockObjectStoreManager = mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS.get());
  private Processor mockFailingMessageProcessor = mock(Processor.class, RETURNS_DEEP_STUBS.get());
  private Processor mockWaitingMessageProcessor = mock(Processor.class, RETURNS_DEEP_STUBS.get());
  private InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS.get());
  private CoreEvent event;
  private Latch waitLatch = new Latch();
  private CountDownLatch waitingMessageProcessorExecutionLatch = new CountDownLatch(2);
  private ExpressionManager expressionManager = mock(ExpressionManager.class);
  private final IdempotentRedeliveryPolicy irp = new IdempotentRedeliveryPolicy();
  private AtomicInteger count = new AtomicInteger();
  private ObjectStore mockObjectStore = mock(ObjectStore.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  @SuppressWarnings("rawtypes")
  public void setUpTest() throws MuleException {
    event = spy(testEvent());
    when(mockFailingMessageProcessor.apply(any(Publisher.class)))
        .thenAnswer(invocation -> {
          MessagingException me = mock(MessagingException.class, RETURNS_DEEP_STUBS.get());
          CoreEvent event = mock(CoreEvent.class);
          when(event.getError()).thenReturn(of(mock(Error.class)));
          when(me.getEvent()).thenReturn(event);
          return error(me).doOnError(e -> count.getAndIncrement());
        });
    when(mockWaitingMessageProcessor.apply(any(Publisher.class))).thenAnswer(invocationOnMock -> {
      Mono<CoreEvent> mono = from(invocationOnMock.getArgumentAt(0, Publisher.class));
      return mono.doOnNext(checkedConsumer(event1 -> {
        waitingMessageProcessorExecutionLatch.countDown();
        waitLatch.await(2000, MILLISECONDS);
      })).transform(mockFailingMessageProcessor);
    });
    MuleLockFactory muleLockFactory = new MuleLockFactory();
    muleLockFactory.setLockProvider(new SingleServerLockProvider());
    muleLockFactory.initialise();
    when(mockMuleContext.getConfiguration().getDefaultEncoding()).thenReturn(UTF_8.name());
    final InMemoryObjectStore inMemoryObjectStore = new InMemoryObjectStore();
    when(mockObjectStoreManager.getObjectStore(anyString())).thenReturn(inMemoryObjectStore);
    when(mockObjectStoreManager.createObjectStore(any(), any())).thenReturn(inMemoryObjectStore);
    when(event.getMessage()).thenReturn(message);

    IdempotentRedeliveryPolicyTestCase.serializer = SerializationTestUtils.getJavaSerializerWithMockContext();

    irp.setExpressionManager(expressionManager);
    irp.setMaxRedeliveryCount(MAX_REDELIVERY_COUNT);
    irp.setUseSecureHash(true);
    irp.setMuleContext(mockMuleContext);
    irp.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    irp.setListener(mockFailingMessageProcessor);

    irp.setLockFactory(muleLockFactory);
    irp.setObjectStoreManager(mockObjectStoreManager);
  }

  @Test
  public void messageDigestFailure() throws Exception {
    when(expressionManager.evaluate(anyString(), any(DataType.class), any(BindingContext.class), any(CoreEvent.class)))
        .thenThrow(ExpressionRuntimeException.class);
    when(message.getPayload()).thenReturn(new TypedValue<>(new Object(), OBJECT));
    irp.initialise();
    CoreEvent process = irp.process(event);
    assertThat(process, nullValue());
  }

  @Test
  public void testMessageRedeliveryUsingMemory() throws Exception {
    when(expressionManager.evaluate(eq(format(SECURE_HASH_EXPR_FORMAT, "SHA-256")), eq(STRING), eq(NULL_BINDING_CONTEXT), any()))
        .thenAnswer(inv -> {
          return new TypedValue<>("" + inv.getArgumentAt(3, CoreEvent.class).getMessage().getPayload().hashCode(), STRING);
        });

    when(message.getPayload()).thenReturn(new TypedValue<>(STRING_MESSAGE, STRING));
    irp.initialise();
    processUntilFailure();
    assertThat(count.get(), equalTo(MAX_REDELIVERY_COUNT + 1));
  }

  @Test
  public void testMessageRedeliveryUsingSerializationStore() throws Exception {
    when(expressionManager.evaluate(eq(format(SECURE_HASH_EXPR_FORMAT, "SHA-256")), eq(STRING), eq(NULL_BINDING_CONTEXT), any()))
        .thenAnswer(inv -> {
          return new TypedValue<>("" + inv.getArgumentAt(3, CoreEvent.class).getMessage().getPayload().hashCode(), STRING);
        });

    when(message.getPayload()).thenReturn(new TypedValue<>(STRING_MESSAGE, STRING));
    reset(mockObjectStoreManager);
    final ObjectStore serializationObjectStore = new SerializationObjectStore();
    when(mockObjectStoreManager.createObjectStore(any(), any())).thenReturn(serializationObjectStore);
    irp.initialise();
    processUntilFailure();
    assertThat(count.get(), equalTo(MAX_REDELIVERY_COUNT + 1));
  }

  @Test
  public void testThreadSafeObjectStoreUsage() throws Exception {
    when(expressionManager.evaluate(eq(format(SECURE_HASH_EXPR_FORMAT, "SHA-256")), eq(STRING), eq(NULL_BINDING_CONTEXT), any()))
        .thenAnswer(inv -> {
          return new TypedValue<>("" + inv.getArgumentAt(3, CoreEvent.class).getMessage().getPayload().hashCode(), STRING);
        });

    when(message.getPayload()).thenReturn(new TypedValue<>(STRING_MESSAGE, STRING));
    irp.setListener(mockWaitingMessageProcessor);
    irp.initialise();
    ExecuteIrpThread firstIrpExecutionThread = new ExecuteIrpThread();
    firstIrpExecutionThread.start();
    ExecuteIrpThread threadCausingRedeliveryException = new ExecuteIrpThread();
    threadCausingRedeliveryException.start();
    waitingMessageProcessorExecutionLatch.await(5000, MILLISECONDS);
    waitLatch.release();
    firstIrpExecutionThread.join();
    threadCausingRedeliveryException.join();
    assertThat(count.get(), equalTo(2));
  }

  @Test
  public void multipleObjectStoreConfigurationShouldRaiseException() throws Exception {
    irp.setObjectStore(mockObjectStore);
    irp.setPrivateObjectStore(mockObjectStore);
    expectedException.expect(InitialisationException.class);
    irp.initialise();
  }

  private void processUntilFailure() {
    for (int i = 0; i < MAX_REDELIVERY_COUNT + 2; i++) {
      try {
        irp.process(event);
      } catch (Exception e) {
        // ignore exception
      }
    }
  }

  public class ExecuteIrpThread extends Thread {

    public Exception exception;

    @Override
    public void run() {
      try {
        irp.process(event);
      } catch (Exception e) {
        exception = e;
      }
    }
  }

  public static class SerializationObjectStore extends TemplateObjectStore<RedeliveryCounter> {

    private Map<String, Serializable> store = new HashMap<>();

    @Override
    protected boolean doContains(String key) throws ObjectStoreException {
      return store.containsKey(key);
    }

    @Override
    protected void doStore(String key, RedeliveryCounter value) throws ObjectStoreException {
      store.put(key, serializer.getExternalProtocol().serialize(value));
    }

    @Override
    protected RedeliveryCounter doRetrieve(String key) throws ObjectStoreException {
      Serializable serializable = store.get(key);
      return serializer.getExternalProtocol().deserialize((byte[]) serializable);
    }

    @Override
    protected RedeliveryCounter doRemove(String key) throws ObjectStoreException {
      Serializable serializable = store.remove(key);
      return serializer.getExternalProtocol().deserialize((byte[]) serializable);
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public void clear() throws ObjectStoreException {
      this.store.clear();
    }

    @Override
    public void open() throws ObjectStoreException {

    }

    @Override
    public void close() throws ObjectStoreException {

    }

    @Override
    public List<String> allKeys() throws ObjectStoreException {
      return new ArrayList<>(store.keySet());
    }

    @Override
    public Map<String, RedeliveryCounter> retrieveAll() throws ObjectStoreException {
      return store.entrySet().stream().collect(
                                               toMap(entry -> entry.getKey(),
                                                     entry -> (RedeliveryCounter) serializer.getExternalProtocol()
                                                         .deserialize((byte[]) entry.getValue())));
    }
  }

  public static class InMemoryObjectStore extends TemplateObjectStore<RedeliveryCounter> {

    private Map<String, RedeliveryCounter> store = new HashMap<>();

    @Override
    protected boolean doContains(String key) throws ObjectStoreException {
      return store.containsKey(key);
    }

    @Override
    protected void doStore(String key, RedeliveryCounter value) throws ObjectStoreException {
      store.put(key, value);
    }

    @Override
    protected RedeliveryCounter doRetrieve(String key) throws ObjectStoreException {
      return store.get(key);
    }

    @Override
    protected RedeliveryCounter doRemove(String key) throws ObjectStoreException {
      return store.remove(key);
    }

    @Override
    public void clear() throws ObjectStoreException {
      this.store.clear();
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public void open() throws ObjectStoreException {

    }

    @Override
    public void close() throws ObjectStoreException {

    }

    @Override
    public List<String> allKeys() throws ObjectStoreException {
      return new ArrayList<>(store.keySet());
    }

    @Override
    public Map<String, RedeliveryCounter> retrieveAll() throws ObjectStoreException {
      return unmodifiableMap(store);
    }
  }
}
