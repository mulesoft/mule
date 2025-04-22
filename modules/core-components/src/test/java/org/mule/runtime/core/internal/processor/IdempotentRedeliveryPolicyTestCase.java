/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.internal.processor.IdempotentRedeliveryPolicy.SECURE_HASH_EXPR_FORMAT;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.REDELIVERY;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.processor.IdempotentRedeliveryPolicy.RedeliveryCounter;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.reactivestreams.Publisher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import reactor.core.publisher.Mono;

@Feature(SOURCES)
@Story(REDELIVERY)
public class IdempotentRedeliveryPolicyTestCase extends AbstractMuleContextTestCase {

  public static final String STRING_MESSAGE = "message";
  public static final int MAX_REDELIVERY_COUNT = 5;
  private static ObjectSerializer serializer;

  private final ObjectStoreManager mockObjectStoreManager = mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS);
  private final Processor mockFailingMessageProcessor = mock(Processor.class, RETURNS_DEEP_STUBS);
  private final Processor mockWaitingMessageProcessor = mock(Processor.class, RETURNS_DEEP_STUBS);
  private final InternalMessage message = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
  private final Latch waitLatch = new Latch();
  private final CountDownLatch waitingMessageProcessorExecutionLatch = new CountDownLatch(2);
  private final IdempotentRedeliveryPolicy irp = new IdempotentRedeliveryPolicy();
  private final AtomicInteger count = new AtomicInteger();
  private final ObjectStore<RedeliveryCounter> mockObjectStore = mock(ObjectStore.class);
  private final InMemoryObjectStore inMemoryObjectStore = spy(new InMemoryObjectStore());
  private CoreEvent event;
  private ExpressionManager expressionManager;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Before
  @SuppressWarnings("rawtypes")
  public void setUpTest() throws MuleException {
    event = spy(testEvent());
    expressionManager = spy(muleContext.getExpressionManager());

    when(mockFailingMessageProcessor.apply(any(Publisher.class)))
        .thenAnswer(invocation -> {
          MessagingException me = mock(MessagingException.class, RETURNS_DEEP_STUBS);
          CoreEvent event = mock(CoreEvent.class);
          when(event.getError()).thenReturn(of(mock(Error.class)));
          when(me.getEvent()).thenReturn(event);
          return error(me).doOnError(e -> count.getAndIncrement());
        });
    when(mockWaitingMessageProcessor.apply(any(Publisher.class))).thenAnswer(invocationOnMock -> {
      Mono<CoreEvent> mono = from(invocationOnMock.getArgument(0));
      return mono.doOnNext(checkedConsumer(event1 -> {
        waitingMessageProcessorExecutionLatch.countDown();
        waitLatch.await(2000, MILLISECONDS);
      })).transform(mockFailingMessageProcessor);
    });

    when(mockObjectStoreManager.getObjectStore(anyString())).thenReturn(inMemoryObjectStore);
    when(mockObjectStoreManager.createObjectStore(any(), any())).thenReturn(inMemoryObjectStore);
    when(event.getMessage()).thenReturn(message);

    IdempotentRedeliveryPolicyTestCase.serializer = SerializationTestUtils.getJavaSerializerWithMockContext();

    muleContext.getInjector().inject(irp);
    irp.setExpressionManager(expressionManager);
    irp.setMaxRedeliveryCount(MAX_REDELIVERY_COUNT);
    irp.setUseSecureHash(true);
    irp.setMuleContext(muleContext);
    irp.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    irp.setMessageProcessors(singletonList(mockFailingMessageProcessor));

    final LockFactory lockFactory = mock(LockFactory.class);
    when(lockFactory.createLock(anyString())).thenReturn(new ReentrantLock());
    irp.setLockFactory(lockFactory);
    irp.setObjectStoreManager(mockObjectStoreManager);
  }

  @After
  public void after() {
    disposeIfNeeded(irp, getLogger(getClass()));
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void messageDigestFailure() throws Exception {
    when(expressionManager.openSession(any())).thenThrow(new ExpressionRuntimeException(createStaticMessage("mock")));
    when(message.getPayload()).thenReturn(new TypedValue<>(new Object(), OBJECT));
    irp.initialise();
    irp.process(event);
  }

  @Test
  public void testMessageRedeliveryUsingMemory() throws Exception {
    mockSha256();

    when(message.getPayload()).thenReturn(new TypedValue<>(STRING_MESSAGE, STRING));
    irp.initialise();
    processUntilFailure();
    assertThat(count.get(), equalTo(MAX_REDELIVERY_COUNT + 1));
  }

  @Test
  public void testMessageRedeliveryUsingSerializationStore() throws Exception {
    mockSha256();

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
    mockSha256();

    when(message.getPayload()).thenReturn(new TypedValue<>(STRING_MESSAGE, STRING));
    irp.setMessageProcessors(singletonList(mockWaitingMessageProcessor));
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
    assertThrows(InitialisationException.class, () -> irp.initialise());
  }

  @Test
  public void javaObject() throws MuleException {
    final Object payloadValue = mock(Object.class);
    event = spy(CoreEvent.builder(testEvent()).addVariable("hash", payloadValue.hashCode()).build());

    irp.setIdExpression("#[vars.hash]");
    irp.initialise();

    when(message.getPayload()).thenReturn(new TypedValue<>(payloadValue, OBJECT));
    processUntilFailure();

    verify(payloadValue).hashCode();
  }

  @Test
  @Issue("W-11985583")
  public void objectStoreIsClosedOnDisposeWhenItIsOwnedByTheRedeliveryPolicy() throws Exception {
    irp.setPrivateObjectStore(mockObjectStore);
    irp.initialise();
    irp.start();
    irp.stop();
    irp.dispose();
    verify(mockObjectStore).close();
  }

  @Test
  @Issue("W-11985583")
  public void objectStoreIsRemovedOnDisposeWhenItIsOwnedByTheRedeliveryPolicy() throws Exception {
    irp.setPrivateObjectStore(mockObjectStore);
    irp.initialise();
    irp.start();
    irp.stop();
    irp.dispose();
    verify(mockObjectStoreManager)
        .disposeStore(TEST_CONNECTOR_LOCATION.getRootContainerName() + "." + IdempotentRedeliveryPolicy.class.getName());
  }

  @Test
  @Issue("W-11985583")
  public void objectStoreIsClosedOnDisposeWhenItIsTheImplicitOne() throws Exception {
    irp.initialise();
    irp.start();
    irp.stop();
    irp.dispose();
    verify(inMemoryObjectStore).close();
  }

  @Test
  @Issue("W-11985583")
  public void objectStoreIsRemovedOnDisposeWhenItIsTheImplicitOne() throws Exception {
    irp.initialise();
    irp.start();
    irp.stop();
    irp.dispose();
    verify(mockObjectStoreManager)
        .disposeStore(TEST_CONNECTOR_LOCATION.getRootContainerName() + "." + IdempotentRedeliveryPolicy.class.getName());
  }

  @Test
  @Issue("W-11985583")
  public void objectStoreIsNotClosedOnDisposeWhenTheRedeliveryPolicyReferencesItByName() throws Exception {
    irp.setObjectStore(mockObjectStore);
    irp.initialise();
    irp.start();
    irp.stop();
    irp.dispose();
    verify(mockObjectStore, never()).close();
  }

  @Test
  @Issue("W-11985583")
  public void objectStoreIsNotRemovedOnDisposeWhenTheRedeliveryPolicyReferencesItByName() throws Exception {
    irp.setObjectStore(mockObjectStore);
    irp.initialise();
    irp.start();
    irp.stop();
    irp.dispose();
    verify(mockObjectStoreManager, never())
        .disposeStore(TEST_CONNECTOR_LOCATION.getRootContainerName() + "." + IdempotentRedeliveryPolicy.class.getName());
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

    private final Map<String, Serializable> store = new HashMap<>();

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
                                               toMap(Entry::getKey,
                                                     entry -> (RedeliveryCounter) serializer.getExternalProtocol()
                                                         .deserialize((byte[]) entry.getValue())));
    }
  }

  private void mockSha256() {
    ExpressionManager expressionManager = mock(ExpressionManager.class);
    ExpressionManagerSession session = mock(ExpressionManagerSession.class);
    CompiledExpression compiledExpression = mock(CompiledExpression.class);

    when(expressionManager.openSession(any())).thenReturn(session);
    when(expressionManager.compile(eq(format(SECURE_HASH_EXPR_FORMAT, "SHA-256")), any())).thenReturn(compiledExpression);
    irp.setExpressionManager(expressionManager);
    when(session.evaluate(compiledExpression, STRING))
        .thenAnswer(inv -> new TypedValue<>("" + event.getMessage().getPayload().hashCode(), STRING));
  }

  public static class InMemoryObjectStore extends TemplateObjectStore<RedeliveryCounter> {

    private final Map<String, RedeliveryCounter> store = new HashMap<>();

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
