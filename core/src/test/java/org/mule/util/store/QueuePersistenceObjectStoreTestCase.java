/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.config.DefaultMuleConfiguration.failIfDeleteOpenFile;
import static org.mule.config.DefaultMuleConfiguration.shouldFailIfDeleteOpenFile;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.serialization.SerializationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.tck.NonSerializableObject;
import org.mule.util.FileUtils;
import org.mule.util.UUID;
import org.mule.util.queue.objectstore.QueueKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class QueuePersistenceObjectStoreTestCase extends AbstractObjectStoreContractTestCase
{
    private static final String QUEUE_NAME = "the-queue";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File persistenceFolder;
    private MuleContext mockMuleContext;
    private boolean originalFailIfDeleteOpenFile;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        initMockMuleContext();
        originalFailIfDeleteOpenFile = shouldFailIfDeleteOpenFile();
        failIfDeleteOpenFile = true;
    }

    @Override
    protected void doTearDown()
    {
        failIfDeleteOpenFile = originalFailIfDeleteOpenFile;
    }

    private void initMockMuleContext() throws IOException
    {
        persistenceFolder = tempFolder.newFolder("persistence");

        MuleConfiguration mockConfig = mock(MuleConfiguration.class);
        when(mockConfig.getWorkingDirectory()).thenReturn(persistenceFolder.getAbsolutePath());

        mockMuleContext = mock(MuleContext.class);
        when(mockMuleContext.getConfiguration()).thenReturn(mockConfig);
        when(mockMuleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
        addJavaSerializerToMockMuleContext(mockMuleContext);
    }

    @Override
    public QueuePersistenceObjectStore<Serializable> getObjectStore() throws ObjectStoreException
    {
        QueuePersistenceObjectStore<Serializable> store =
            new QueuePersistenceObjectStore<Serializable>(mockMuleContext);
        store.open();
        return store;
    }

    @Override
    public Serializable getStorableValue()
    {
        return new DefaultMuleMessage(TEST_MESSAGE, muleContext);
    }

    @Override
    protected Serializable createKey()
    {
        return new QueueKey("theQueue", UUID.getUUID());
    }

    @Test
    public void testCreatingTheObjectStoreThrowsMuleRuntimeException()
    {
        MuleRuntimeException muleRuntimeException = new MuleRuntimeException(CoreMessages.createStaticMessage("boom"));

        MuleContext mockContext = mock(MuleContext.class);
        when(mockContext.getConfiguration()).thenThrow(muleRuntimeException);

        QueuePersistenceObjectStore<Serializable> store =
            new QueuePersistenceObjectStore<Serializable>(mockContext);

        try
        {
            store.open();
            fail();
        }
        catch (ObjectStoreException ose)
        {
            // this one was expected
        }
    }

    @Test
    public void testAllKeysOnNotYetOpenedStore() throws ObjectStoreException
    {
        QueuePersistenceObjectStore<Serializable> store =
            new QueuePersistenceObjectStore<Serializable>(mockMuleContext);

        List<Serializable> allKeys = store.allKeys();
        assertEquals(0, allKeys.size());
    }

    @Test
    public void testListExistingFiles() throws Exception
    {
        QueuePersistenceObjectStore<Serializable> store = getObjectStore();

        String id = UUID.getUUID();
        createAndPopulateStoreFile(id, TEST_MESSAGE);

        List<Serializable> allKeys = store.allKeys();
        assertEquals(1, allKeys.size());

        QueueKey key = (QueueKey)allKeys.get(0);
        assertEquals(id, key.id);
    }

    @Test
    public void testRetrieveFileFromDisk() throws Exception
    {
        // create the store first so that the queuestore directory is created as a side effect
        QueuePersistenceObjectStore<Serializable> store = getObjectStore();

        String id = UUID.getUUID();
        createAndPopulateStoreFile(id, TEST_MESSAGE);

        QueueKey key = new QueueKey(QUEUE_NAME, id);
        Serializable value = store.retrieve(key);
        assertEquals(TEST_MESSAGE, value);
    }

    @Test
    public void testRemove() throws Exception
    {
        // create the store first so that the queuestore directory is created as a side effect
        QueuePersistenceObjectStore<Serializable> store = getObjectStore();

        String id = UUID.getUUID();
        File storeFile = createAndPopulateStoreFile(id, TEST_MESSAGE);

        QueueKey key = new QueueKey(QUEUE_NAME, id);
        store.remove(key);

        assertFalse(storeFile.exists());
    }

    @Test
    public void testMonitoredWrapper() throws Exception
    {
        QueuePersistenceObjectStore<Serializable> store = getObjectStore(); 
        String id = UUID.getUUID();
        QueueKey key = new QueueKey(QUEUE_NAME, id);
        MuleMessage msg = new DefaultMuleMessage("Hello", mockMuleContext);
        MuleEvent event = new DefaultMuleEvent(msg, MessageExchangePattern.ONE_WAY, (FlowConstruct) null);

        ListableObjectStore<Serializable> monitored = new MonitoredObjectStoreWrapper(store);
        monitored.store(key, event);
        MonitoredObjectStoreWrapper.StoredObject  retrieved = (MonitoredObjectStoreWrapper.StoredObject) store.retrieve(key);
        Object item = retrieved.getItem();
        assertTrue(item instanceof MuleEvent);
        MuleEvent newEvent = (MuleEvent) item;
        MuleMessage newMessage = newEvent.getMessage();
        assertNotNull(newMessage);
        assertEquals(mockMuleContext, newMessage.getMuleContext());
        assertEquals("Hello", newMessage.getPayload());
    }

    @Test
    public void queueFilesAreRemovedWhenSerializationFails() throws ObjectStoreException
    {
        QueuePersistenceObjectStore<Serializable> store = getObjectStore();
        String id = UUID.getUUID();
        Serializable value = new SerializableWrapper(new NonSerializableObject());
        File queueFile = createStoreFile(id);

        try
        {
            store.store(new QueueKey(QUEUE_NAME, id), value);
            fail();
        }
        catch (ObjectStoreException e)
        {
            assertThat(e.getCause(), instanceOf(SerializationException.class));
            assertThat(queueFile.exists(), is(false));
        }
    }

    private File createAndPopulateStoreFile(String id, String payload) throws IOException
    {
        File storeFile = createStoreFile(id);

        // create the directory for the queue
        storeFile.getParentFile().mkdir();

        FileOutputStream fos = new FileOutputStream(storeFile);
        muleContext.getObjectSerializer().serialize(payload, fos);

        return storeFile;
    }

    private File createStoreFile(String id)
    {
        String path = String.format("%1s/%2s/%3s/%4s.msg", persistenceFolder.getAbsolutePath(),
            QueuePersistenceObjectStore.DEFAULT_QUEUE_STORE, QUEUE_NAME, id);
        return FileUtils.newFile(path);
    }


    private static class SerializableWrapper implements Serializable
    {
        Object data;

        SerializableWrapper(Object data)
        {
            this.data = data;
        }
    }

}
