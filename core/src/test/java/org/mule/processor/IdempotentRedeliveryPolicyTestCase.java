/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;
import org.mule.util.lock.MuleLockFactory;
import org.mule.util.lock.SingleServerLockProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IdempotentRedeliveryPolicyTestCase extends AbstractMuleTestCase
{

    public static final String STRING_MESSAGE = "message";
    public static final int MAX_REDELIVERY_COUNT = 0;
    private static ObjectSerializer serializer;

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private ObjectStoreManager mockObjectStoreManager = mock(ObjectStoreManager.class, Answers.RETURNS_DEEP_STUBS.get());
    private MessageProcessor mockFailingMessageProcessor = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private MessageProcessor mockWaitingMessageProcessor = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private MessageProcessor mockDlqMessageProcessor = mock(MessageProcessor.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleMessage message = mock(MuleMessage.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent event = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
    private Latch waitLatch = new Latch();
    private CountDownLatch waitingMessageProcessorExecutionLatch = new CountDownLatch(2);
    private final IdempotentRedeliveryPolicy irp = new IdempotentRedeliveryPolicy();

    @Rule
    public SystemProperty systemProperty = new SystemProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY,"utf-8");

    @Before
    @SuppressWarnings("rawtypes")
    public void setUpTest() throws MuleException
    {
        when(mockFailingMessageProcessor.process(any(MuleEvent.class))).thenThrow(new RuntimeException("failing"));
        when(mockWaitingMessageProcessor.process(event)).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                waitingMessageProcessorExecutionLatch.countDown();
                waitLatch.await(2000, TimeUnit.MILLISECONDS);
                return mockFailingMessageProcessor.process((MuleEvent) invocationOnMock.getArguments()[0]);
            }
        });
        MuleLockFactory muleLockFactory = new MuleLockFactory();
        muleLockFactory.setMuleContext(mockMuleContext);
        when(mockMuleContext.getRegistry().get(MuleProperties.OBJECT_LOCK_PROVIDER)).thenReturn(new SingleServerLockProvider());
        muleLockFactory.initialise();
        when(mockMuleContext.getLockFactory()).thenReturn(muleLockFactory);
        when(mockMuleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).thenReturn(mockObjectStoreManager);
        final InMemoryObjectStore inMemoryObjectStore = new InMemoryObjectStore();
        when(mockObjectStoreManager.getObjectStore(anyString(), anyBoolean(), anyInt(), anyInt(), anyInt())).thenAnswer(new Answer<ObjectStore>()
        {
            @Override
            public ObjectStore answer(InvocationOnMock invocation) throws Throwable
            {
                return inMemoryObjectStore;
            }
        });
        when(event.getMessage()).thenReturn(message);

        IdempotentRedeliveryPolicyTestCase.serializer = SerializationTestUtils.getJavaSerializerWithMockContext();

        irp.setMaxRedeliveryCount(MAX_REDELIVERY_COUNT);
        irp.setUseSecureHash(true);
        irp.setFlowConstruct(mock(FlowConstruct.class));
        irp.setMuleContext(mockMuleContext);
        irp.setListener(mockFailingMessageProcessor);
        irp.setMessageProcessor(mockDlqMessageProcessor);

    }

    @Test
    public void messageDigestFailure() throws Exception
    {
        when(message.getPayload()).thenReturn(new Object());
        irp.initialise();
        MuleEvent process = irp.process(event);
        Assert.assertNull(process);
    }

    @Test
    public void testMessageRedeliveryUsingMemory() throws Exception
    {
        when(message.getPayload()).thenReturn(STRING_MESSAGE);
        irp.initialise();
        processUntilFailure();
        verify(mockDlqMessageProcessor, VerificationModeFactory.times(1)).process(event);
    }

    @Test
    public void testMessageRedeliveryUsingSerializationStore() throws Exception
    {
        when(message.getPayload()).thenReturn(STRING_MESSAGE);
        reset(mockObjectStoreManager);
        final ObjectStore serializationObjectStore = new SerializationObjectStore();
        when(mockObjectStoreManager.getObjectStore(anyString(), anyBoolean(), anyInt(), anyInt(), anyInt())).thenAnswer(new Answer<ObjectStore>()
        {
            @Override
            public ObjectStore answer(InvocationOnMock invocation) throws Throwable
            {
                return serializationObjectStore;
            }
        });
        irp.initialise();
        processUntilFailure();
        verify(mockDlqMessageProcessor, VerificationModeFactory.times(1)).process(event);
    }

    @Test
    public void testThreadSafeObjectStoreUsage() throws Exception
    {
        when(message.getPayload()).thenReturn(STRING_MESSAGE);
        irp.setListener(mockWaitingMessageProcessor);
        irp.initialise();
        ExecuteIrpThread firstIrpExecutionThread = new ExecuteIrpThread();
        firstIrpExecutionThread.start();
        ExecuteIrpThread threadCausingRedeliveryException = new ExecuteIrpThread();
        threadCausingRedeliveryException.start();
        waitingMessageProcessorExecutionLatch.await(5000, TimeUnit.MILLISECONDS);
        waitLatch.release();
        firstIrpExecutionThread.join();
        threadCausingRedeliveryException.join();
        verify(mockDlqMessageProcessor, VerificationModeFactory.times(1)).process(event);
    }

    private void processUntilFailure()
    {
        for (int i = 0; i < MAX_REDELIVERY_COUNT + 2; i++)
        {
            try
            {
                irp.process(event);
            }
            catch (Exception e)
            {
                // ignore exception
            }
        }
    }

    public class ExecuteIrpThread extends Thread
    {
        public Exception exception;

        @Override
        public void run()
        {
            try
            {
                irp.process(event);
            }
            catch (Exception e)
            {
                exception = e;
            }
        }
    }

    public static class SerializationObjectStore implements ObjectStore<AtomicInteger>
    {
        private Map<Serializable,Serializable> store = new HashMap<Serializable,Serializable>();

        @Override
        public boolean contains(Serializable key) throws ObjectStoreException
        {
            return store.containsKey(key);
        }

        @Override
        public void store(Serializable key, AtomicInteger value) throws ObjectStoreException
        {
            store.put(key, serializer.serialize(value));
        }

        @Override
        public AtomicInteger retrieve(Serializable key) throws ObjectStoreException
        {
            Serializable serializable = store.get(key);
            return serializer.deserialize((byte[]) serializable);
        }

        @Override
        public AtomicInteger remove(Serializable key) throws ObjectStoreException
        {
            Serializable serializable = store.remove(key);
            return serializer.deserialize((byte[]) serializable);
        }

        @Override
        public boolean isPersistent()
        {
            return false;
        }

        @Override
        public void clear() throws ObjectStoreException
        {
            this.store.clear();
        }
    }

    public static class InMemoryObjectStore implements ObjectStore<AtomicInteger>
    {
        private Map<Serializable,AtomicInteger> store = new HashMap<Serializable,AtomicInteger>();

        @Override
        public boolean contains(Serializable key) throws ObjectStoreException
        {
            return store.containsKey(key);
        }

        @Override
        public void store(Serializable key, AtomicInteger value) throws ObjectStoreException
        {
            store.put(key,value);
        }

        @Override
        public AtomicInteger retrieve(Serializable key) throws ObjectStoreException
        {
            return store.get(key);
        }

        @Override
        public AtomicInteger remove(Serializable key) throws ObjectStoreException
        {
            return store.remove(key);
        }

        @Override
        public void clear() throws ObjectStoreException
        {
            this.store.clear();
        }

        @Override
        public boolean isPersistent()
        {
            return false;
        }
    }
}
