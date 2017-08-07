/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.core.internal.lock.SingleServerLockProvider;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class IdempotentRedeliveryPolicyTestCase extends AbstractMuleTestCase {

  public static final String STRING_MESSAGE = "message";
  public static final int MAX_REDELIVERY_COUNT = 5;
  private static final String UTF_8 = "utf-8";
  private static ObjectSerializer serializer;

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
  private ObjectStoreManager mockObjectStoreManager = mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS.get());
  private Processor mockFailingMessageProcessor = mock(Processor.class, RETURNS_DEEP_STUBS.get());
  private Processor mockWaitingMessageProcessor = mock(Processor.class, RETURNS_DEEP_STUBS.get());
  private InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS.get());
  private Event event;
  private Latch waitLatch = new Latch();
  private CountDownLatch waitingMessageProcessorExecutionLatch = new CountDownLatch(2);
  private final IdempotentRedeliveryPolicy irp = new IdempotentRedeliveryPolicy();
  private AtomicInteger count = new AtomicInteger();

  @Before
  @SuppressWarnings("rawtypes")
  public void setUpTest() throws MuleException {
    event = spy(testEvent());
    when(mockFailingMessageProcessor.apply(any(Publisher.class)))
        .thenAnswer(invocation -> error(new RuntimeException("failing"))
            .doOnError(e -> count.getAndIncrement()));
    when(mockWaitingMessageProcessor.apply(any(Publisher.class))).thenAnswer(invocationOnMock -> {
      Mono<Event> mono = from(invocationOnMock.getArgumentAt(0, Publisher.class));
      return mono.doOnNext(checkedConsumer(event1 -> {
        waitingMessageProcessorExecutionLatch.countDown();
        waitLatch.await(2000, MILLISECONDS);
      })).transform(mockFailingMessageProcessor);
    });
    MuleLockFactory muleLockFactory = new MuleLockFactory();
    muleLockFactory.setMuleContext(mockMuleContext);
    when(mockMuleContext.getRegistry().get(MuleProperties.OBJECT_LOCK_PROVIDER)).thenReturn(new SingleServerLockProvider());
    muleLockFactory.initialise();
    when(mockMuleContext.getLockFactory()).thenReturn(muleLockFactory);
    when(mockMuleContext.getObjectStoreManager()).thenReturn(mockObjectStoreManager);
    when(mockMuleContext.getConfiguration().getDefaultEncoding()).thenReturn(UTF_8);
    final InMemoryObjectStore inMemoryObjectStore = new InMemoryObjectStore();
    when(mockObjectStoreManager.getObjectStore(anyString())).thenReturn(inMemoryObjectStore);
    when(mockObjectStoreManager.createObjectStore(any(), any())).thenReturn(inMemoryObjectStore);
    when(event.getMessage()).thenReturn(message);

    IdempotentRedeliveryPolicyTestCase.serializer = SerializationTestUtils.getJavaSerializerWithMockContext();

    irp.setMaxRedeliveryCount(MAX_REDELIVERY_COUNT);
    irp.setUseSecureHash(true);
    irp.setMuleContext(mockMuleContext);
    irp.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    irp.setListener(mockFailingMessageProcessor);
  }

  @Test
  public void messageDigestFailure() throws Exception {
    when(message.getPayload()).thenReturn(new TypedValue<>(new Object(), OBJECT));
    irp.initialise();
    Event process = irp.process(event);
    assertThat(process, nullValue());
  }

  @Test
  public void testMessageRedeliveryUsingMemory() throws Exception {
    when(message.getPayload()).thenReturn(new TypedValue<>(STRING_MESSAGE, STRING));
    irp.initialise();
    processUntilFailure();
    assertThat(count.get(), equalTo(MAX_REDELIVERY_COUNT + 1));
  }

  @Test
  public void testMessageRedeliveryUsingSerializationStore() throws Exception {
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

  public static class SerializationObjectStore extends TemplateObjectStore<AtomicInteger> {

    private Map<String, Serializable> store = new HashMap<>();

    @Override
    protected boolean doContains(String key) throws ObjectStoreException {
      return store.containsKey(key);
    }

    @Override
    protected void doStore(String key, AtomicInteger value) throws ObjectStoreException {
      store.put(key, serializer.getExternalProtocol().serialize(value));
    }

    @Override
    protected AtomicInteger doRetrieve(String key) throws ObjectStoreException {
      Serializable serializable = store.get(key);
      return serializer.getExternalProtocol().deserialize((byte[]) serializable);
    }

    @Override
    protected AtomicInteger doRemove(String key) throws ObjectStoreException {
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
  }

  public static class InMemoryObjectStore extends TemplateObjectStore<AtomicInteger> {

    private Map<String, AtomicInteger> store = new HashMap<>();

    @Override
    protected boolean doContains(String key) throws ObjectStoreException {
      return store.containsKey(key);
    }

    @Override
    protected void doStore(String key, AtomicInteger value) throws ObjectStoreException {
      store.put(key, value);
    }

    @Override
    protected AtomicInteger doRetrieve(String key) throws ObjectStoreException {
      return store.get(key);
    }

    @Override
    protected AtomicInteger doRemove(String key) throws ObjectStoreException {
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
  }
}
