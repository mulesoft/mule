/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.store;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.util.FileUtils.newFile;
import static org.mule.runtime.core.util.store.QueuePersistenceObjectStore.DEFAULT_QUEUE_STORE;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.core.util.queue.objectstore.QueueKey;
import org.mule.tck.NonSerializableObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class QueuePersistenceObjectStoreTestCase extends AbstractObjectStoreContractTestCase {

  private static final String QUEUE_NAME = "the-queue";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private File persistenceFolder;
  private MuleContext mockMuleContext;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    initMockMuleContext();
  }

  private void initMockMuleContext() throws IOException {
    persistenceFolder = tempFolder.newFolder("persistence");

    MuleConfiguration mockConfig = mock(MuleConfiguration.class);
    when(mockConfig.getWorkingDirectory()).thenReturn(persistenceFolder.getAbsolutePath());

    mockMuleContext = spy(muleContext);
    when(mockMuleContext.getConfiguration()).thenReturn(mockConfig);
    when(mockMuleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
    addJavaSerializerToMockMuleContext(mockMuleContext);
  }

  @Override
  public QueuePersistenceObjectStore<Serializable> getObjectStore() throws ObjectStoreException {
    QueuePersistenceObjectStore<Serializable> store = new QueuePersistenceObjectStore<>(mockMuleContext);
    store.open();
    return store;
  }

  @Override
  public Serializable getStorableValue() {
    return MuleMessage.builder().payload(TEST_MESSAGE).build();
  }

  @Override
  protected Serializable createKey() {
    return new QueueKey("theQueue", UUID.getUUID());
  }

  @Test
  public void testCreatingTheObjectStoreThrowsMuleRuntimeException() {
    MuleRuntimeException muleRuntimeException = new MuleRuntimeException(CoreMessages.createStaticMessage("boom"));

    MuleContext mockContext = mock(MuleContext.class);
    when(mockContext.getConfiguration()).thenThrow(muleRuntimeException);

    QueuePersistenceObjectStore<Serializable> store = new QueuePersistenceObjectStore<>(mockContext);

    try {
      store.open();
      fail();
    } catch (ObjectStoreException ose) {
      // this one was expected
    }
  }

  @Test
  public void testAllKeysOnNotYetOpenedStore() throws ObjectStoreException {
    QueuePersistenceObjectStore<Serializable> store = new QueuePersistenceObjectStore<>(mockMuleContext);

    List<Serializable> allKeys = store.allKeys();
    assertEquals(0, allKeys.size());
  }

  @Test
  public void testListExistingFiles() throws Exception {
    QueuePersistenceObjectStore<Serializable> store = getObjectStore();

    String id = UUID.getUUID();
    createAndPopulateStoreFile(id, TEST_MESSAGE);

    List<Serializable> allKeys = store.allKeys();
    assertEquals(1, allKeys.size());

    QueueKey key = (QueueKey) allKeys.get(0);
    assertEquals(id, key.id);
  }

  @Test
  public void testRetrieveFileFromDisk() throws Exception {
    // create the store first so that the queuestore directory is created as a side effect
    QueuePersistenceObjectStore<Serializable> store = getObjectStore();

    String id = UUID.getUUID();
    createAndPopulateStoreFile(id, TEST_MESSAGE);

    QueueKey key = new QueueKey(QUEUE_NAME, id);
    Serializable value = store.retrieve(key);
    assertEquals(TEST_MESSAGE, value);
  }

  @Test
  public void testRemove() throws Exception {
    // create the store first so that the queuestore directory is created as a side effect
    QueuePersistenceObjectStore<Serializable> store = getObjectStore();

    String id = UUID.getUUID();
    File storeFile = createAndPopulateStoreFile(id, TEST_MESSAGE);

    QueueKey key = new QueueKey(QUEUE_NAME, id);
    store.remove(key);

    assertFalse(storeFile.exists());
  }

  @Test
  public void testMonitoredWrapper() throws Exception {
    QueuePersistenceObjectStore<Serializable> store = getObjectStore();
    String id = UUID.getUUID();
    QueueKey key = new QueueKey(QUEUE_NAME, id);
    MuleMessage msg = MuleMessage.builder().payload("Hello").build();
    Flow flow = getTestFlow();
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(msg).exchangePattern(ONE_WAY)
        .flow(flow).build();

    ListableObjectStore<Serializable> monitored = new MonitoredObjectStoreWrapper(store);
    monitored.store(key, event);
    MonitoredObjectStoreWrapper.StoredObject retrieved = (MonitoredObjectStoreWrapper.StoredObject) store.retrieve(key);
    Object item = retrieved.getItem();
    assertTrue(item instanceof MuleEvent);
    MuleEvent newEvent = (MuleEvent) item;
    MuleMessage newMessage = newEvent.getMessage();
    assertNotNull(newMessage);
    assertEquals("Hello", newMessage.getPayload());
  }

  @Test
  public void queueFilesAreRemovedWhenSerializationFails() throws ObjectStoreException {
    QueuePersistenceObjectStore<Serializable> store = getObjectStore();
    String id = UUID.getUUID();
    Serializable value = new SerializableWrapper(new NonSerializableObject());
    File queueFile = createStoreFile(id);

    try {
      store.store(new QueueKey(QUEUE_NAME, id), value);
      fail();
    } catch (ObjectStoreException e) {
      assertThat(e.getCause(), instanceOf(SerializationException.class));
      assertThat(queueFile.exists(), is(false));
    }
  }

  private File createAndPopulateStoreFile(String id, String payload) throws IOException {
    File storeFile = createStoreFile(id);

    // create the directory for the queue
    storeFile.getParentFile().mkdir();

    FileOutputStream fos = new FileOutputStream(storeFile);
    muleContext.getObjectSerializer().serialize(payload, fos);

    return storeFile;
  }

  private File createStoreFile(String id) {
    String path = format("%1s/%2s/%3s/%4s.msg", persistenceFolder.getAbsolutePath(), DEFAULT_QUEUE_STORE, QUEUE_NAME, id);
    return newFile(path);
  }

  private static class SerializableWrapper implements Serializable {

    Object data;

    SerializableWrapper(Object data) {
      this.data = data;
    }
  }

}
